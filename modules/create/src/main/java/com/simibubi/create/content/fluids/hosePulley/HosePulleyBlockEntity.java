package com.simibubi.create.content.fluids.hosePulley;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class HosePulleyBlockEntity extends KineticBlockEntity implements SidedStorageBlockEntity {

	LerpedFloat offset;
	boolean isMoving;

	private SmartFluidTank internalTank;
	private FluidDrainingBehaviour drainer;
	private FluidFillingBehaviour filler;
	private HosePulleyFluidHandler handler;
	private boolean infinite;

	public HosePulleyBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
		offset = LerpedFloat.linear()
			.startWithValue(0);
		isMoving = true;					// fabric: we hold twice forge, due to differences in logic
											// the handler refills from the world when stored > BUCKET, which could result in nearly 2 buckets worth.
											// on forge, there's no risk of negative results with stored > capacity
											// fabric does have this issue, so we can *not* allow it.
		internalTank = new SmartFluidTank(FluidConstants.BUCKET * 3, this::onTankContentsChanged);
		handler = new HosePulleyFluidHandler(internalTank, filler, drainer,
			() -> worldPosition.below((int) Math.ceil(offset.getValue())), () -> !this.isMoving);
	}

	@Override
	public void sendData() {
		infinite = filler.isInfinite() || drainer.isInfinite();
		super.sendData();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean addToGoggleTooltip = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		if (infinite)
			TooltipHelper.addHint(tooltip, "hint.hose_pulley");
		return addToGoggleTooltip;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		drainer = new FluidDrainingBehaviour(this);
		filler = new FluidFillingBehaviour(this);
		behaviours.add(drainer);
		behaviours.add(filler);
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.HOSE_PULLEY, AllAdvancements.HOSE_PULLEY_LAVA);
	}

	protected void onTankContentsChanged(FluidStack contents) {}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		isMoving = true;
		if (getSpeed() == 0) {
			offset.forceNextSync();
			offset.setValue(Math.round(offset.getValue()));
			isMoving = false;
		}

		if (isMoving) {
			float newOffset = offset.getValue() + getMovementSpeed();
			if (newOffset < 0)
				isMoving = false;
			if (!level.getBlockState(worldPosition.below((int) Math.ceil(newOffset)))
				.canBeReplaced()) {
				isMoving = false;
			}
			if (isMoving) {
				drainer.reset(null);
				filler.reset(null);
			}
		}

		super.onSpeedChanged(previousSpeed);
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return super.createRenderBoundingBox().expandTowards(0, -offset.getValue(), 0);
	}

	@Override
	public void tick() {
		super.tick();
		float newOffset = offset.getValue() + getMovementSpeed();
		if (newOffset < 0) {
			newOffset = 0;
			isMoving = false;
		}
		if (!level.getBlockState(worldPosition.below((int) Math.ceil(newOffset)))
			.canBeReplaced()) {
			newOffset = (int) newOffset;
			isMoving = false;
		}
		if (getSpeed() == 0)
			isMoving = false;

		offset.setValue(newOffset);
		invalidateRenderBoundingBox();
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level.isClientSide)
			return;
		if (isMoving)
			return;

		int ceil = (int) Math.ceil(offset.getValue() + getMovementSpeed());
		if (getMovementSpeed() > 0 && level.getBlockState(worldPosition.below(ceil))
			.canBeReplaced()) {
			isMoving = true;
			drainer.reset(null);
			filler.reset(null);
			return;
		}

		sendData();
	}

	@Override
	protected void write(CompoundTag compound, boolean clientPacket) {
		if (clientPacket)
			offset.forceNextSync();
		compound.put("Offset", offset.writeNBT());
		compound.put("Tank", internalTank.writeToNBT(new CompoundTag()));
		super.write(compound, clientPacket);
		if (clientPacket)
			compound.putBoolean("Infinite", infinite);
	}

	@Override
	protected void read(CompoundTag compound, Provider reg, boolean clientPacket) {
		offset.readNBT(compound.getCompound("Offset"), clientPacket);
		internalTank.readFromNBT(compound.getCompound("Tank"));
		super.read(compound, reg, clientPacket);
		if (clientPacket)
			infinite = compound.getBoolean("Infinite");
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	public float getMovementSpeed() {
		float movementSpeed = convertToLinear(getSpeed());
		if (level.isClientSide)
			movementSpeed *= ServerSpeedProvider.get();
		return movementSpeed;
	}

	public float getInterpolatedOffset(float pt) {
		return offset.getValue(pt);
	}

	@Nullable
	@Override
	public Storage<FluidVariant> getFluidStorage(@Nullable Direction face) {
		if (face == null || HosePulleyBlock.hasPipeTowards(level, worldPosition, getBlockState(), face)) {
			return handler;
		}
		return null;
	}
}
