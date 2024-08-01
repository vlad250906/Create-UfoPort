package com.simibubi.create.content.fluids.spout;

import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.HOLD;
import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.PASS;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.BlockSpoutingBehaviour;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidFX;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import io.github.fabricators_of_create.porting_lib.block.CustomRenderBoundingBoxBlockEntity;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SpoutBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, SidedStorageBlockEntity, CustomRenderBoundingBoxBlockEntity {

	public static final int FILLING_TIME = 20;
	protected BeltProcessingBehaviour beltProcessing;

	public int processingTicks;
	public boolean sendSplash;
	public BlockSpoutingBehaviour customProcess;

	SmartFluidTankBehaviour tank;

	private boolean createdSweetRoll, createdHoneyApple, createdChocolateBerries;

	public SpoutBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		processingTicks = -1;
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return super.createRenderBoundingBox().expandTowards(0, -2, 0);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		tank = SmartFluidTankBehaviour.single(this, FluidConstants.BUCKET);
		behaviours.add(tank);

		beltProcessing = new BeltProcessingBehaviour(this).whenItemEnters(this::onItemReceived)
			.whileItemHeld(this::whenItemHeld);
		behaviours.add(beltProcessing);

		registerAwardables(behaviours, AllAdvancements.SPOUT, AllAdvancements.FOODS);
	}

	protected ProcessingResult onItemReceived(TransportedItemStack transported,
		TransportedItemStackHandlerBehaviour handler) {
		if (handler.blockEntity.isVirtual())
			return PASS;
		if (!FillingBySpout.canItemBeFilled(level, transported.stack))
			return PASS;
		if (tank.isEmpty())
			return HOLD;
		if (FillingBySpout.getRequiredAmountForItem(level, transported.stack, getCurrentFluidInTank()) == -1)
			return PASS;
		return HOLD;
	}

	protected ProcessingResult whenItemHeld(TransportedItemStack transported,
		TransportedItemStackHandlerBehaviour handler) {
		if (processingTicks != -1 && processingTicks != 5)
			return HOLD;
		if (!FillingBySpout.canItemBeFilled(level, transported.stack))
			return PASS;
		if (tank.isEmpty())
			return HOLD;
		FluidStack fluid = getCurrentFluidInTank();
		long requiredAmountForItem = FillingBySpout.getRequiredAmountForItem(level, transported.stack, fluid.copy());
		if (requiredAmountForItem == -1)
			return PASS;
		if (requiredAmountForItem > fluid.getAmount())
			return HOLD;

		if (processingTicks == -1) {
			processingTicks = FILLING_TIME;
			notifyUpdate();
			return HOLD;
		}

		// Process finished
		ItemStack out = FillingBySpout.fillItem(level, requiredAmountForItem, transported.stack, fluid);
		if (!out.isEmpty()) {
			List<TransportedItemStack> outList = new ArrayList<>();
			TransportedItemStack held = null;
			TransportedItemStack result = transported.copy();
			result.stack = out;
			if (!transported.stack.isEmpty())
				held = transported.copy();
			outList.add(result);
			handler.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(outList, held));
		}

		award(AllAdvancements.SPOUT);
		if (trackFoods()) {
			createdChocolateBerries |= AllItems.CHOCOLATE_BERRIES.isIn(out);
			createdHoneyApple |= AllItems.HONEYED_APPLE.isIn(out);
			createdSweetRoll |= AllItems.SWEET_ROLL.isIn(out);
			if (createdChocolateBerries && createdHoneyApple && createdSweetRoll)
				award(AllAdvancements.FOODS);
		}

		tank.getPrimaryHandler()
			.setFluid(fluid.isEmpty() ? FluidStack.EMPTY : fluid); // fabric: if the FluidStack is empty it should actually be empty
		sendSplash = true;
		notifyUpdate();
		return HOLD;
	}

	private FluidStack getCurrentFluidInTank() {
		return tank.getPrimaryHandler()
			.getFluid();
	}

	@Override
	protected void write(CompoundTag compound, boolean clientPacket) {
		super.write(compound, clientPacket);

		compound.putInt("ProcessingTicks", processingTicks);
		if (sendSplash && clientPacket) {
			compound.putBoolean("Splash", true);
			sendSplash = false;
		}

		if (!trackFoods())
			return;
		if (createdChocolateBerries)
			NBTHelper.putMarker(compound, "ChocolateBerries");
		if (createdHoneyApple)
			NBTHelper.putMarker(compound, "HoneyApple");
		if (createdSweetRoll)
			NBTHelper.putMarker(compound, "SweetRoll");
	}

	private boolean trackFoods() {
		return getBehaviour(AdvancementBehaviour.TYPE).isOwnerPresent();
	}
	
	@Override
	protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		processingTicks = compound.getInt("ProcessingTicks");

		createdChocolateBerries = compound.contains("ChocolateBerries");
		createdHoneyApple = compound.contains("HoneyApple");
		createdSweetRoll = compound.contains("SweetRoll");

		if (!clientPacket)
			return;
		if (compound.contains("Splash"))
			spawnSplash(tank.getPrimaryTank()
				.getRenderedFluid());
	}

	@Nullable
	@Override
	public Storage<FluidVariant> getFluidStorage(@Nullable Direction face) {
		if (face != Direction.DOWN) {
			return tank.getCapability();
		}
		return null;
	}

	public void tick() {
		super.tick();

		FluidStack currentFluidInTank = getCurrentFluidInTank();
		if (processingTicks == -1 && (isVirtual() || !level.isClientSide()) && !currentFluidInTank.isEmpty()) {
			BlockSpoutingBehaviour.forEach(behaviour -> {
				if (customProcess != null)
					return;
				if (behaviour.fillBlock(level, worldPosition.below(2), this, currentFluidInTank, true) > 0) {
					processingTicks = FILLING_TIME;
					customProcess = behaviour;
					notifyUpdate();
				}
			});
		}

		if (processingTicks >= 0) {
			processingTicks--;
			if (processingTicks == 5 && customProcess != null) {
				long fillBlock = customProcess.fillBlock(level, worldPosition.below(2), this, currentFluidInTank, false);
				customProcess = null;
				if (fillBlock > 0) {
					// fabric: if the FluidStack is empty it should actually be empty
					FluidStack newStack = FluidHelper.copyStackWithAmount(currentFluidInTank,
							currentFluidInTank.getAmount() - fillBlock);
					if (newStack.isEmpty())
						newStack = FluidStack.EMPTY;
					tank.getPrimaryHandler()
						.setFluid(newStack);
					sendSplash = true;
					notifyUpdate();
				}
			}
		}

		if (processingTicks >= 8 && level.isClientSide)
			spawnProcessingParticles(tank.getPrimaryTank()
				.getRenderedFluid());
	}

	protected void spawnProcessingParticles(FluidStack fluid) {
		if (isVirtual())
			return;
		Vec3 vec = VecHelper.getCenterOf(worldPosition);
		vec = vec.subtract(0, 8 / 16f, 0);
		ParticleOptions particle = FluidFX.getFluidParticle(fluid);
		level.addAlwaysVisibleParticle(particle, vec.x, vec.y, vec.z, 0, -.1f, 0);
	}

	protected static int SPLASH_PARTICLE_COUNT = 20;

	protected void spawnSplash(FluidStack fluid) {
		if (isVirtual())
			return;
		Vec3 vec = VecHelper.getCenterOf(worldPosition);
		vec = vec.subtract(0, 2 - 5 / 16f, 0);
		ParticleOptions particle = FluidFX.getFluidParticle(fluid);
		for (int i = 0; i < SPLASH_PARTICLE_COUNT; i++) {
			Vec3 m = VecHelper.offsetRandomly(Vec3.ZERO, level.random, 0.125f);
			m = new Vec3(m.x, Math.abs(m.y), m.z);
			level.addAlwaysVisibleParticle(particle, vec.x, vec.y, vec.z, m.x, m.y, m.z);
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return containedFluidTooltip(tooltip, isPlayerSneaking, getFluidStorage(null));
	}
}
