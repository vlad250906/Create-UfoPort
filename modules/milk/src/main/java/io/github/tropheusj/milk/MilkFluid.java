package io.github.tropheusj.milk;

import static io.github.tropheusj.milk.Milk.FLOWING_MILK;
import static io.github.tropheusj.milk.Milk.MILK_CAULDRON;
import static io.github.tropheusj.milk.Milk.MILK_FLUID_BLOCK;
import static io.github.tropheusj.milk.Milk.STILL_MILK;
import static net.minecraft.world.item.Items.MILK_BUCKET;

import java.util.Optional;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import org.jetbrains.annotations.Nullable;

import io.github.tropheusj.dripstone_fluid_lib.DripstoneInteractingFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;

public abstract class MilkFluid extends FlowingFluid implements DripstoneInteractingFluid {

	@Override
	public Fluid getSource() {
		return STILL_MILK;
	}

	@Override
	public Fluid getFlowing() {
		return FLOWING_MILK;
	}

	@Override
	public Item getBucket() {
		return MILK_BUCKET;
	}

	@Override
	protected int getSlopeFindDistance(LevelReader worldView) {
		return 2;
	}

	@Override
	protected float getExplosionResistance() {
		return 100.0F;
	}

	@Override
	protected boolean canBeReplacedWith(FluidState fluidState, BlockGetter blockView, BlockPos blockPos, Fluid fluid, Direction direction) {
		return false;
	}

	@Override
	public int getTickDelay(LevelReader worldView) {
		return 5;
	}

	@Override
	protected int getDropOff(LevelReader worldView) {
		return 1;
	}
	
	@Override
	protected void beforeDestroyingBlock(LevelAccessor world, BlockPos pos, BlockState state) {
		final BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
		Block.dropResources(state, world, pos, blockEntity);
	}

	@Override
	protected boolean canConvertToSource(Level world) {
		return Milk.INFINITE_MILK_FLUID;
	}

	@Override
	public boolean isSame(Fluid fluid) {
		return fluid == getSource() || fluid == getFlowing();
	}

	@Override
	public Optional<SoundEvent> getPickupSound() {
		return Optional.of(SoundEvents.BUCKET_FILL);
	}

	@Override
	public int getParticleColor(Level world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
		return 0xFFFFFF;
	}

	@Override
	public boolean growsDripstone(BlockState state) {
		return true;
	}

	@Override
	public boolean fillsCauldrons(BlockState state, Level world, BlockPos cauldronPos) {
		return MILK_CAULDRON != null;
	}

	@Override
	public @Nullable BlockState getCauldronBlockState(BlockState state, Level world, BlockPos cauldronPos) {
		return MILK_CAULDRON.defaultBlockState();
	}
	

	@Override
	public float getFluidDripChance(Level world, PointedDripstoneBlock.FluidInfo drippingFluid) {
		return WATER_DRIP_CHANCE;
	}
	
	@Override
	protected BlockState createLegacyBlock(FluidState fluidState) {
		return MILK_FLUID_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, MilkFluid.getLegacyLevel(fluidState));
	}

	public static class Flowing extends MilkFluid {

		@Override
		public int getAmount(FluidState fluidState) {
			return fluidState.getValue(LEVEL);
		}

		@Override
		public boolean isSource(FluidState fluidState) {
			return false;
		}
		
		@Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
		
		
	}

	public static class Still extends MilkFluid {
		@Override
		public int getAmount(FluidState fluidState) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState fluidState) {
			return true;
		}
	}
}
