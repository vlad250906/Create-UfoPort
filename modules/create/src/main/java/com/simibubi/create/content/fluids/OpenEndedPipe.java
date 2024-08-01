package com.simibubi.create.content.fluids;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.simibubi.create.AllFluids;
import com.simibubi.create.content.fluids.pipes.VanillaFluidTargets;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.tags.Tags;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import io.github.tropheusj.milk.Milk;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

public class OpenEndedPipe extends FlowSource {

	private static final List<IEffectHandler> EFFECT_HANDLERS = new ArrayList<>();

	static {
		registerEffectHandler(new PotionEffectHandler());
		registerEffectHandler(new MilkEffectHandler());
		registerEffectHandler(new WaterEffectHandler());
		registerEffectHandler(new LavaEffectHandler());
		registerEffectHandler(new TeaEffectHandler());
	}

	private Level world;
	private BlockPos pos;
	private AABB aoe;

	private OpenEndFluidHandler fluidHandler;
	private BlockPos outputPos;
	private boolean wasPulling;

	private FluidStack cachedFluid;
	private List<MobEffectInstance> cachedEffects;

	public OpenEndedPipe(BlockFace face) {
		super(face);
		fluidHandler = new OpenEndFluidHandler();
		outputPos = face.getConnectedPos();
		pos = face.getPos();
		aoe = new AABB(outputPos).expandTowards(0, -1, 0);
		if (face.getFace() == Direction.DOWN)
			aoe = aoe.expandTowards(0, -1, 0);
	}

	public static void registerEffectHandler(IEffectHandler handler) {
		EFFECT_HANDLERS.add(handler);
	}

	public Level getWorld() {
		return world;
	}

	public BlockPos getPos() {
		return pos;
	}

	public BlockPos getOutputPos() {
		return outputPos;
	}

	public AABB getAOE() {
		return aoe;
	}

	@Override
	public void manageSource(Level world) {
		this.world = world;
	}

	@Override
	public Storage<FluidVariant> provideHandler() {
		return fluidHandler;
	}

	@Override
	public boolean isEndpoint() {
		return true;
	}

	public CompoundTag serializeNBT() {
		CompoundTag compound = new CompoundTag();
		fluidHandler.writeToNBT(compound);
		compound.putBoolean("Pulling", wasPulling);
		compound.put("Location", location.serializeNBT());
		return compound;
	}

	public static OpenEndedPipe fromNBT(CompoundTag compound, BlockPos blockEntityPos) {
		BlockFace fromNBT = BlockFace.fromNBT(compound.getCompound("Location"));
		OpenEndedPipe oep = new OpenEndedPipe(new BlockFace(blockEntityPos, fromNBT.getFace()));
		oep.fluidHandler.readFromNBT(compound);
		oep.wasPulling = compound.getBoolean("Pulling");
		return oep;
	}

	private FluidStack removeFluidFromSpace(TransactionContext ctx) {
		FluidStack empty = FluidStack.EMPTY;
		if (world == null)
			return empty;
		if (!world.isLoaded(outputPos))
			return empty;

		BlockState state = world.getBlockState(outputPos);
		FluidState fluidState = state.getFluidState();
		boolean waterlog = state.hasProperty(WATERLOGGED);

		FluidStack drainBlock = VanillaFluidTargets.drainBlock(world, outputPos, state, ctx);
		if (!drainBlock.isEmpty()) {
			if (state.hasProperty(BlockStateProperties.LEVEL_HONEY)
				&& AllFluids.HONEY.is(drainBlock.getFluid()))
				TransactionCallback.onSuccess(ctx, () -> AdvancementBehaviour.tryAward(world, pos, AllAdvancements.HONEY_DRAIN));
			return drainBlock;
		}

		if (!waterlog && !state.canBeReplaced())
			return empty;
		if (fluidState.isEmpty() || !fluidState.isSource())
			return empty;

		FluidStack stack = new FluidStack(fluidState.getType(), FluidConstants.BUCKET);

		if (FluidHelper.isWater(stack.getFluid()))
			AdvancementBehaviour.tryAward(world, pos, AllAdvancements.WATER_SUPPLY);

		world.updateSnapshots(ctx);
		if (waterlog) {
			world.setBlock(outputPos, state.setValue(WATERLOGGED, false), 3);
			TransactionCallback.onSuccess(ctx, () -> world.scheduleTick(outputPos, Fluids.WATER, 1));
			return stack;
		}
		world.setBlock(outputPos, fluidState.createLegacyBlock()
			.setValue(LiquidBlock.LEVEL, 14), 3);
		return stack;
	}

	private boolean provideFluidToSpace(FluidStack fluid, TransactionContext ctx) {
		if (world == null)
			return false;
		if (!world.isLoaded(outputPos))
			return false;

		BlockState state = world.getBlockState(outputPos);
		FluidState fluidState = state.getFluidState();
		boolean waterlog = state.hasProperty(WATERLOGGED);

		if (!waterlog && !state.canBeReplaced())
			return false;
		if (fluid.isEmpty())
			return false;
		if (!FluidHelper.hasBlockState(fluid.getFluid()) || fluid.getFluid().is(Milk.MILK_FLUID_TAG)) // fabric: milk logic is different
			return true;

		// fabric: note - this is possibly prone to issues but follows what forge does.
		// collisions completely ignore simulation / transactions.
		if (!fluidState.isEmpty() && fluidState.getType() != fluid.getFluid()) {
			FluidReactions.handlePipeSpillCollision(world, outputPos, fluid.getFluid(), fluidState);
			return false;
		}

		if (fluidState.isSource())
			return false;
		if (waterlog && fluid.getFluid() != Fluids.WATER)
			return false;

		if (world.dimensionType()
			.ultraWarm() && FluidHelper.isTag(fluid, FluidTags.WATER)) {
			int i = outputPos.getX();
			int j = outputPos.getY();
			int k = outputPos.getZ();
			TransactionCallback.onSuccess(ctx, () -> world.playSound(null, i, j, k, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
					2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F));
			return true;
		}

		world.updateSnapshots(ctx);
		if (waterlog) {
			world.setBlock(outputPos, state.setValue(WATERLOGGED, true), 3);
			TransactionCallback.onSuccess(ctx, () -> world.scheduleTick(outputPos, Fluids.WATER, 1));
			return true;
		}

		if (!AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get())
			return true;

		world.setBlock(outputPos, fluid.getFluid()
			.defaultFluidState()
			.createLegacyBlock(), 3);
		return true;
	}

	private boolean canApplyEffects(FluidStack fluid) {
		for (IEffectHandler handler : EFFECT_HANDLERS) {
			if (handler.canApplyEffects(this, fluid)) {
				return true;
			}
		}
		return false;
	}

	private void applyEffects(FluidStack fluid) {
		for (IEffectHandler handler : EFFECT_HANDLERS) {
			if (handler.canApplyEffects(this, fluid)) {
				handler.applyEffects(this, fluid);
			}
		}
	}

	private class OpenEndFluidHandler extends FluidTank {

		public OpenEndFluidHandler() {
			super(FluidConstants.BUCKET);
		}

		@Override
		public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			// Never allow being filled when a source is attached
			if (world == null)
				return 0;
			if (!world.isLoaded(outputPos))
				return 0;
			if (resource.isBlank())
				return 0;
			FluidStack stack = new FluidStack(resource, 81);
			updateSnapshots(transaction);
			try (Transaction provideTest = transaction.openNested()) {
				if (!provideFluidToSpace(stack, provideTest))
					return 0;
			}

			FluidStack containedFluidStack = getFluid();
			boolean hasBlockState = FluidHelper.hasBlockState(containedFluidStack.getFluid());

			if (!containedFluidStack.isEmpty() && !containedFluidStack.canFill(resource))
				setFluid(FluidStack.EMPTY);
			if (wasPulling)
				wasPulling = false;

			if (canApplyEffects(stack) && !hasBlockState)
				maxAmount = 81; // fabric: deplete fluids 81 times faster to account for larger amounts
			long fill = super.insert(resource, maxAmount, transaction);
			if (!stack.isEmpty())
				TransactionCallback.onSuccess(transaction, () -> applyEffects(stack));
			if (getFluidAmount() == FluidConstants.BUCKET || (!FluidHelper.hasBlockState(containedFluidStack.getFluid()) || containedFluidStack.getFluid().is(Milk.MILK_FLUID_TAG))) { // fabric: milk logic is different
				if (provideFluidToSpace(containedFluidStack, transaction))
					setFluid(FluidStack.EMPTY);
			}
			return fill;
		}

		@Override
		public long extract(FluidVariant extractedVariant, long maxAmount, TransactionContext transaction) {
			if (world == null)
				return 0;
			if (!world.isLoaded(outputPos))
				return 0;
			if (maxAmount == 0)
				return 0;
			if (maxAmount > FluidConstants.BUCKET) {
				maxAmount = FluidConstants.BUCKET;
			}

			if (!wasPulling)
				wasPulling = true;

			updateSnapshots(transaction);
			long drainedFromInternal = super.extract(extractedVariant, maxAmount, transaction);
			if (drainedFromInternal != 0)
				return drainedFromInternal;

			FluidStack drainedFromWorld = removeFluidFromSpace(transaction);
			if (drainedFromWorld.isEmpty())
				return 0;
			if (!drainedFromWorld.canFill(extractedVariant))
				return 0;

			long remainder = drainedFromWorld.getAmount() - maxAmount;
			drainedFromWorld.setAmount(maxAmount);

			if (remainder > 0) {
				if (!getFluid().isEmpty() && !getFluid().isFluidEqual(drainedFromWorld))
					setFluid(FluidStack.EMPTY);
				super.insert(drainedFromWorld.getType(), remainder, transaction);
			}
			return drainedFromWorld.getAmount();
		}

		@Override
		public boolean isResourceBlank() {
			if (!super.isResourceBlank()) return false;
			return getResource().isBlank();
		}

		@Override
		public FluidVariant getResource() {
			if (!super.isResourceBlank()) return super.getResource();
			try (Transaction t = TransferUtil.getTransaction()) {
				FluidStack stack = removeFluidFromSpace(t);
				return stack.getType();
			}
		}

		@Override
		public long getAmount() {
			long amount = super.getAmount();
			if (amount != 0) return amount;
			return isResourceBlank() ? 0 : FluidConstants.BUCKET;
		}
	}

	public interface IEffectHandler {
		boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid);

		void applyEffects(OpenEndedPipe pipe, FluidStack fluid);
	}

	public static class PotionEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return fluid.getFluid()
				.isSame(AllFluids.POTION.get());
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			if (pipe.cachedFluid == null || pipe.cachedEffects == null || !fluid.isFluidEqual(pipe.cachedFluid)) {
				FluidStack copy = fluid.copy();
				copy.setAmount(FluidConstants.BOTTLE);
				ItemStack bottle = PotionFluidHandler.fillBottle(new ItemStack(Items.GLASS_BOTTLE), fluid);
				Iterable<MobEffectInstance> itable = bottle.has(DataComponents.POTION_CONTENTS) ? bottle.get(DataComponents.POTION_CONTENTS).getAllEffects() : null;
				pipe.cachedEffects = new ArrayList<MobEffectInstance>();
				if(itable != null) {
					Iterator<MobEffectInstance> iter = itable.iterator();
					while(iter.hasNext()) {
						pipe.cachedEffects.add(iter.next());
					}
				}
			}

			if (pipe.cachedEffects.isEmpty())
				return;

			List<LivingEntity> entities = pipe.getWorld()
				.getEntitiesOfClass(LivingEntity.class, pipe.getAOE(), LivingEntity::isAffectedByPotions);
			for (LivingEntity entity : entities) {
				for (MobEffectInstance effectInstance : pipe.cachedEffects) {
					MobEffect effect = effectInstance.getEffect().value();
					if (effect.isInstantenous()) {
						effect.applyInstantenousEffect(null, null, entity, effectInstance.getAmplifier(), 0.5D);
					} else {
						entity.addEffect(new MobEffectInstance(effectInstance));
					}
				}
			}
		}
	}

	public static class MilkEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return FluidHelper.isTag(fluid, Tags.Fluids.MILK);
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			Level world = pipe.getWorld();
			if (world.getGameTime() % 5 != 0)
				return;
			List<LivingEntity> entities =
				world.getEntitiesOfClass(LivingEntity.class, pipe.getAOE(), LivingEntity::isAffectedByPotions);
//			ItemStack curativeItem = new ItemStack(Items.MILK_BUCKET);
			for (LivingEntity entity : entities)
				entity.removeAllEffects();
		}
	}

	public static class WaterEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return FluidHelper.isTag(fluid, FluidTags.WATER);
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			Level world = pipe.getWorld();
			if (world.getGameTime() % 5 != 0)
				return;
			List<Entity> entities = world.getEntities((Entity) null, pipe.getAOE(), Entity::isOnFire);
			for (Entity entity : entities)
				entity.clearFire();
			BlockPos.betweenClosedStream(pipe.getAOE())
				.forEach(pos -> dowseFire(world, pos));
		}

		// Adapted from ThrownPotion
		private static void dowseFire(Level level, BlockPos pos) {
			BlockState state = level.getBlockState(pos);
			if (state.is(BlockTags.FIRE)) {
				level.removeBlock(pos, false);
			} else if (AbstractCandleBlock.isLit(state)) {
				AbstractCandleBlock.extinguish(null, state, level, pos);
			} else if (CampfireBlock.isLitCampfire(state)) {
				level.levelEvent(null, 1009, pos, 0);
				CampfireBlock.dowse(null, level, pos, state);
				level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
			}
		}
	}

	public static class LavaEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return FluidHelper.isTag(fluid, FluidTags.LAVA);
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			Level world = pipe.getWorld();
			if (world.getGameTime() % 5 != 0)
				return;
			List<Entity> entities = world.getEntities((Entity) null, pipe.getAOE(), entity -> !entity.fireImmune());
			for (Entity entity : entities)
				entity.igniteForSeconds(3);
		}
	}

	public static class TeaEffectHandler implements IEffectHandler {
		@Override
		public boolean canApplyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			return fluid.getFluid().isSame(AllFluids.TEA.get());
		}

		@Override
		public void applyEffects(OpenEndedPipe pipe, FluidStack fluid) {
			Level world = pipe.getWorld();
			if (world.getGameTime() % 5 != 0)
				return;
			List<LivingEntity> entities = world
					.getEntitiesOfClass(LivingEntity.class, pipe.getAOE(), LivingEntity::isAffectedByPotions);
			for (LivingEntity entity : entities) {
					entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 21, 0, false, false, false));
			}
		}
	}

}
