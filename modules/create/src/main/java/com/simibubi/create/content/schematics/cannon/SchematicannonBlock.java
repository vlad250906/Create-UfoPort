package com.simibubi.create.content.schematics.cannon;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;

import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SchematicannonBlock extends Block implements IBE<SchematicannonBlockEntity> {

	public SchematicannonBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return AllShapes.SCHEMATICANNON_SHAPE;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
		if (entity != null) {
			withBlockEntityDo(level, pos, be -> {
				be.defaultYaw = (-Mth.floor((entity.getYRot() + (entity.isShiftKeyDown() ? 180.0F : 0.0F)) * 16.0F / 360.0F + 0.5F) & 15) * 360.0F / 16.0F;
			});
		}
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
	public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
			boolean isMoving) {
		withBlockEntityDo(worldIn, pos, be -> be.neighbourCheckCooldown = 0);
	}

	@Override
	public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.hasBlockEntity() || state.getBlock() == newState.getBlock())
			return;

		withBlockEntityDo(worldIn, pos, be -> ItemHelper.dropContents(worldIn, pos, be.inventory));
		worldIn.removeBlockEntity(pos);
	}

	@Override
	public Class<SchematicannonBlockEntity> getBlockEntityClass() {
		return SchematicannonBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SchematicannonBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SCHEMATICANNON.get();
	}

}
