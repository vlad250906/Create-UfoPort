package com.simibubi.create.content.processing.burner;

import net.minecraft.util.RandomSource;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LitBlazeBurnerBlock extends Block implements IWrenchable, BlockPickInteractionAware {

//	public static final ToolAction EXTINGUISH_FLAME_ACTION = ToolAction.get(Create.asResource("extinguish_flame").toString());

	public static final EnumProperty<FlameType> FLAME_TYPE = EnumProperty.create("flame_type", FlameType.class);

	public LitBlazeBurnerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FLAME_TYPE, FlameType.REGULAR));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FLAME_TYPE);
	}
	
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hitResult) {
		
		ItemStack heldItem = player.getItemInHand(hand);
		if(heldItem.isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

		if (heldItem.getItem() instanceof ShovelItem) {// || heldItem.getItem().canPerformAction(heldItem, EXTINGUISH_FLAME_ACTION)) {
			world.playSound(player, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.5f, 2);
			if (world.isClientSide)
				return ItemInteractionResult.SUCCESS;
			heldItem.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
			world.setBlockAndUpdate(pos, AllBlocks.BLAZE_BURNER.getDefaultState());
			return ItemInteractionResult.SUCCESS;
		}

		if (state.getValue(FLAME_TYPE) == FlameType.REGULAR) {
			if (heldItem.is(ItemTags.SOUL_FIRE_BASE_BLOCKS)) {
				world.playSound(player, pos, SoundEvents.SOUL_SAND_PLACE, SoundSource.BLOCKS, 1.0f, world.random.nextFloat() * 0.4F + 0.8F);
				if (world.isClientSide)
					return ItemInteractionResult.SUCCESS;
				world.setBlockAndUpdate(pos, defaultBlockState().setValue(FLAME_TYPE, FlameType.SOUL));
				return ItemInteractionResult.SUCCESS;
			}
		}

		return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
		return AllBlocks.BLAZE_BURNER.get()
			.getShape(state, reader, pos, context);
	}

	@Override
	public ItemStack getPickedStack(BlockState state, BlockGetter view, BlockPos pos, @Nullable Player player, @Nullable HitResult result) {
		return AllItems.EMPTY_BLAZE_BURNER.asStack();
	}

	@Environment(EnvType.CLIENT)
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		world.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true,
			(double) pos.getX() + 0.5D + random.nextDouble() / 3.0D * (double) (random.nextBoolean() ? 1 : -1),
			(double) pos.getY() + random.nextDouble() + random.nextDouble(),
			(double) pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (double) (random.nextBoolean() ? 1 : -1), 0.0D,
			0.07D, 0.0D);

		if (random.nextInt(10) == 0) {
			world.playLocalSound((double) ((float) pos.getX() + 0.5F), (double) ((float) pos.getY() + 0.5F),
				(double) ((float) pos.getZ() + 0.5F), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
				0.25F + random.nextFloat() * .25f, random.nextFloat() * 0.7F + 0.6F, false);
		}

		if (state.getValue(FLAME_TYPE) == FlameType.SOUL) {
			if (random.nextInt(8) == 0) {
				world.addParticle(ParticleTypes.SOUL,
					pos.getX() + 0.5F + random.nextDouble() / 4 * (random.nextBoolean() ? 1 : -1),
					pos.getY() + 0.3F + random.nextDouble() / 2,
					pos.getZ() + 0.5F + random.nextDouble() / 4 * (random.nextBoolean() ? 1 : -1),
					0.0, random.nextDouble() * 0.04 + 0.04, 0.0);
			}
			return;
		}

		if (random.nextInt(5) == 0) {
			for (int i = 0; i < random.nextInt(1) + 1; ++i) {
				world.addParticle(ParticleTypes.LAVA, (double) ((float) pos.getX() + 0.5F),
					(double) ((float) pos.getY() + 0.5F), (double) ((float) pos.getZ() + 0.5F),
					(double) (random.nextFloat() / 2.0F), 5.0E-5D, (double) (random.nextFloat() / 2.0F));
			}
		}
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState p_149740_1_) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level p_180641_2_, BlockPos p_180641_3_) {
		return state.getValue(FLAME_TYPE) == FlameType.REGULAR ? 1 : 2;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter reader, BlockPos pos,
		CollisionContext context) {
		return AllBlocks.BLAZE_BURNER.get()
			.getCollisionShape(state, reader, pos, context);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	public static int getLight(BlockState state) {
		if (state.getValue(FLAME_TYPE) == FlameType.SOUL)
			return 9;
		else
			return 12;
	}

	public enum FlameType implements StringRepresentable {

		REGULAR, SOUL;

		@Override
		public String getSerializedName() {
			return Lang.asId(name());
		}

	}

}
