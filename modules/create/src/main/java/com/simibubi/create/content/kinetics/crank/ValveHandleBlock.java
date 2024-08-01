package com.simibubi.create.content.kinetics.crank;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Couple;

import io.github.fabricators_of_create.porting_lib.util.TagUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@ParametersAreNonnullByDefault
public class ValveHandleBlock extends HandCrankBlock {

	private final DyeColor color;
	private final boolean inCreativeTab;

	public static ValveHandleBlock copper(Properties properties) {
		return new ValveHandleBlock(properties, null, true);
	}

	public static ValveHandleBlock dyed(Properties properties, DyeColor color) {
		return new ValveHandleBlock(properties, color, false);
	}

	private ValveHandleBlock(Properties properties, DyeColor color, boolean inCreativeTab) {
		super(properties);
		this.color = color;
		this.inCreativeTab = inCreativeTab;
	}

	@Override
	public VoxelShape getShape(BlockState pState, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return AllShapes.VALVE_HANDLE.get(pState.getValue(FACING));
	}

	public static InteractionResult onBlockActivated(Player player, Level level, InteractionHand hand,
			BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		BlockState blockState = level.getBlockState(pos);

		if (!(blockState.getBlock() instanceof ValveHandleBlock vhb))
			return InteractionResult.PASS;
		if (!player.mayBuild())
			return InteractionResult.PASS;
		if (AllItems.WRENCH.isIn(player.getItemInHand(hand)) && player.isShiftKeyDown())
			return InteractionResult.PASS;

		if (vhb.clicked(level, pos, blockState, player, hand)) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		if (!(pNewState.getBlock() instanceof ValveHandleBlock))
			super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
	}

	public boolean clicked(Level level, BlockPos pos, BlockState blockState, Player player, InteractionHand hand) {
		ItemStack heldItem = player.getItemInHand(hand);
		DyeColor color = TagUtil.getColorFromStack(heldItem);
		if (color != null && color != this.color) {
			if (!level.isClientSide)
				level.setBlockAndUpdate(pos, BlockHelper.copyProperties(blockState,
						AllBlocks.DYED_VALVE_HANDLES.get(color).getDefaultState()));
			return true;
		}

		onBlockEntityUse(level, pos,
				hcbe -> (hcbe instanceof ValveHandleBlockEntity vhbe) && vhbe.activate(player.isShiftKeyDown())
						? ItemInteractionResult.SUCCESS
						: ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION);
		return true;
	}
	
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level worldIn, BlockPos pos,
			Player player, InteractionHand handIn, BlockHitResult hitResult) {
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}
	
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level worldIn, BlockPos pos, Player player,
			BlockHitResult hitResult) {
		return InteractionResult.PASS;
	}

	@Override
	public BlockEntityType<? extends HandCrankBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.VALVE_HANDLE.get();
	}

	@Override
	public int getRotationSpeed() {
		return 32;
	}

	public static Couple<Integer> getSpeedRange() {
		return Couple.create(32, 32);
	}

}
