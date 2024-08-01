package com.simibubi.create.content.kinetics.steamEngine;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

import java.util.function.Predicate;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Couple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SteamEngineBlock extends FaceAttachedHorizontalDirectionalBlock
	implements SimpleWaterloggedBlock, IWrenchable, IBE<SteamEngineBlockEntity> {

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public SteamEngineBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACE, AttachFace.FLOOR).setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder.add(FACE, FACING, WATERLOGGED));
	}

	@Override
	public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}
	
	@Override
	public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
		return canAttach(pLevel, pPos, getConnectedDirection(pState).getOpposite());
	}

	public static boolean canAttach(LevelReader pReader, BlockPos pPos, Direction pDirection) {
		BlockPos blockpos = pPos.relative(pDirection);
		return pReader.getBlockState(blockpos)
			.getBlock() instanceof FluidTankBlock;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
	}
	
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult ray) {
		
		ItemStack heldItem = player.getItemInHand(hand);
		if(heldItem.isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

		IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
		if (placementHelper.matchesItem(heldItem))
			return placementHelper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);
		return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		
	}
	
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
			BlockHitResult ray) {
		
		ItemStack heldItem = ItemStack.EMPTY;

		IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
		if (placementHelper.matchesItem(heldItem))
			return placementHelper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, InteractionHand.MAIN_HAND, ray).result();
		return InteractionResult.PASS;
		
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor world,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		return state;
	}

	@Override
	public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
		FluidTankBlock.updateBoilerState(pState, pLevel, pPos.relative(getFacing(pState).getOpposite()));
		BlockPos shaftPos = getShaftPos(pState, pPos);
		BlockState shaftState = pLevel.getBlockState(shaftPos);
		if (isShaftValid(pState, shaftState))
			pLevel.setBlock(shaftPos, PoweredShaftBlock.getEquivalent(shaftState), 3);
	}

	@Override
	public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		if (pState.hasBlockEntity() && (!pState.is(pNewState.getBlock()) || !pNewState.hasBlockEntity()))
			pLevel.removeBlockEntity(pPos);
		FluidTankBlock.updateBoilerState(pState, pLevel, pPos.relative(getFacing(pState).getOpposite()));
		BlockPos shaftPos = getShaftPos(pState, pPos);
		BlockState shaftState = pLevel.getBlockState(shaftPos);
		if (AllBlocks.POWERED_SHAFT.has(shaftState))
			pLevel.scheduleTick(shaftPos, shaftState.getBlock(), 1);
	}

	@Override
	public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
		AttachFace face = pState.getValue(FACE);
		Direction direction = pState.getValue(FACING);
		return face == AttachFace.CEILING ? AllShapes.STEAM_ENGINE_CEILING.get(direction.getAxis())
			: face == AttachFace.FLOOR ? AllShapes.STEAM_ENGINE.get(direction.getAxis())
				: AllShapes.STEAM_ENGINE_WALL.get(direction);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		FluidState ifluidstate = level.getFluidState(pos);
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;
		return state.setValue(WATERLOGGED, Boolean.valueOf(ifluidstate.getType() == Fluids.WATER));
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	public static Direction getFacing(BlockState sideState) {
		return getConnectedDirection(sideState);
	}

	public static BlockPos getShaftPos(BlockState sideState, BlockPos pos) {
		return pos.relative(getConnectedDirection(sideState), 2);
	}

	public static boolean isShaftValid(BlockState state, BlockState shaft) {
		return (AllBlocks.SHAFT.has(shaft) || AllBlocks.POWERED_SHAFT.has(shaft))
			&& shaft.getValue(ShaftBlock.AXIS) != getFacing(state).getAxis();
	}

	@Override
	public Class<SteamEngineBlockEntity> getBlockEntityClass() {
		return SteamEngineBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SteamEngineBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.STEAM_ENGINE.get();
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {
		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return AllBlocks.SHAFT::isIn;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return s -> s.getBlock() instanceof SteamEngineBlock;
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			BlockPos shaftPos = SteamEngineBlock.getShaftPos(state, pos);
			BlockState shaft = AllBlocks.SHAFT.getDefaultState();
			for (Direction direction : Direction.orderedByNearest(player)) {
				shaft = shaft.setValue(ShaftBlock.AXIS, direction.getAxis());
				if (isShaftValid(state, shaft))
					break;
			}
			
			BlockState newState = world.getBlockState(shaftPos);
			if (!newState.canBeReplaced())
				return PlacementOffset.fail();

			Axis axis = shaft.getValue(ShaftBlock.AXIS);
			return PlacementOffset.success(shaftPos,
				s -> BlockHelper.copyProperties(s, AllBlocks.POWERED_SHAFT.getDefaultState())
					.setValue(PoweredShaftBlock.AXIS, axis));
		}
	}
	
	public static Couple<Integer> getSpeedRange() {
		return Couple.create(16, 64);
	}

	@Override
	protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
		// TODO Auto-generated method stub
		return null;
	}

}
