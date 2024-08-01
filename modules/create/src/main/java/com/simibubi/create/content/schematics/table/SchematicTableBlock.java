package com.simibubi.create.content.schematics.table;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;

import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SchematicTableBlock extends HorizontalDirectionalBlock implements IBE<SchematicTableBlockEntity> {

	public SchematicTableBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos,
			CollisionContext context) {
		return AllShapes.TABLE_POLE_SHAPE;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return AllShapes.SCHEMATICS_TABLE.get(state.getValue(FACING));
	}
	
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hitResult) {
		
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		
	}
	
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level worldIn, BlockPos pos, Player player,
			BlockHitResult hitResult) {
		
		if (worldIn.isClientSide)
			return InteractionResult.SUCCESS;
		withBlockEntityDo(worldIn, pos,
				be -> NetworkHooks.openScreen((ServerPlayer) player, be, be::sendToMenu));
		return InteractionResult.SUCCESS;
		
	}

	@Override
	public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.hasBlockEntity() || state.getBlock() == newState.getBlock())
			return;

		withBlockEntityDo(worldIn, pos, be -> ItemHelper.dropContents(worldIn, pos, be.inventory));
		worldIn.removeBlockEntity(pos);
	}

	@Override
	public Class<SchematicTableBlockEntity> getBlockEntityClass() {
		return SchematicTableBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SchematicTableBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SCHEMATIC_TABLE.get();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		// TODO Auto-generated method stub
		return null;
	}

}
