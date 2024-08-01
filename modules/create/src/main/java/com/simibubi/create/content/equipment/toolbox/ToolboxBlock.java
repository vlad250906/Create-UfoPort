package com.simibubi.create.content.equipment.toolbox;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.BlockHelper;

import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;
import io.github.fabricators_of_create.porting_lib.util.TagUtil;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ToolboxBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, IBE<ToolboxBlockEntity> {

	protected final DyeColor color;

	public ToolboxBlock(Properties properties, DyeColor color) {
		super(properties);
		this.color = color;
		registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(WATERLOGGED)
			.add(FACING));
	}

	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(worldIn, pos, state, placer, stack);
		if (worldIn.isClientSide)
			return;
		if (stack == null)
			return;
		withBlockEntityDo(worldIn, pos, be -> {
			CompoundTag orCreateTag = ItemHelper.getOrCreateComponent(stack, AllDataComponents.TOOLBOX, new CompoundTag());
			be.readInventory(orCreateTag.getCompound("Inventory"));
			if (orCreateTag.contains("UniqueId"))
				be.setUniqueId(orCreateTag.getUUID("UniqueId"));
			if (stack.has(DataComponents.CUSTOM_NAME))
				be.setCustomName(stack.getHoverName());
		});
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
		if (state.hasBlockEntity() && (!newState.hasBlockEntity() || !(newState.getBlock() instanceof ToolboxBlock)))
			world.removeBlockEntity(pos);
	}

	@Override
	public void attack(BlockState state, Level world, BlockPos pos, Player player) {
		if (player instanceof FakePlayer)
			return;
		if (world.isClientSide)
			return;
		withBlockEntityDo(world, pos, ToolboxBlockEntity::unequipTracked);
		if (world instanceof ServerLevel) {
			ItemStack cloneItemStack = getCloneItemStack(world, pos, state);
			world.destroyBlock(pos, false);
			if (world.getBlockState(pos) != state)
				player.getInventory().placeItemBackInInventory(cloneItemStack);
		}
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
		ItemStack item = new ItemStack(this);
		Optional<ToolboxBlockEntity> blockEntityOptional = getBlockEntityOptional(world, pos);
		CompoundTag tag = ItemHelper.getOrCreateComponent(item, AllDataComponents.TOOLBOX, new CompoundTag());

		CompoundTag inv = blockEntityOptional.map(tb -> tb.inventory.serializeNBT())
			.orElse(new CompoundTag());
		tag.put("Inventory", inv);

		blockEntityOptional.map(tb -> tb.getUniqueId())
			.ifPresent(uid -> tag.putUUID("UniqueId", uid));
		blockEntityOptional.map(ToolboxBlockEntity::getCustomName)
			.ifPresent(arg -> item.set(DataComponents.CUSTOM_NAME, arg));
		return item;
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor world,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		return state;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return AllShapes.TOOLBOX.get(state.getValue(FACING));
	}
	
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hitResult) {
		
		if (player == null || player.isCrouching())
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		
		if(stack.isEmpty()) 
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		
		DyeColor color = TagUtil.getColorFromStack(stack);
		if (color != null && color != this.color) {
			if (world.isClientSide)
				return ItemInteractionResult.SUCCESS;
			BlockState newState = BlockHelper.copyProperties(state, AllBlocks.TOOLBOXES.get(color)
				.getDefaultState());
			world.setBlockAndUpdate(pos, newState);
			return ItemInteractionResult.SUCCESS;
		}

		if (player instanceof FakePlayer)
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		if (world.isClientSide)
			return ItemInteractionResult.SUCCESS;

		withBlockEntityDo(world, pos,
			toolbox -> NetworkHooks.openScreen((ServerPlayer) player, toolbox, toolbox::sendToMenu));
		return ItemInteractionResult.SUCCESS;
		
	}
	
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
			BlockHitResult hitResult) {
		
		if (player == null || player.isCrouching())
			return InteractionResult.PASS;

		ItemStack stack = ItemStack.EMPTY;
		DyeColor color = TagUtil.getColorFromStack(stack);
		if (color != null && color != this.color) {
			if (world.isClientSide)
				return InteractionResult.SUCCESS;
			BlockState newState = BlockHelper.copyProperties(state, AllBlocks.TOOLBOXES.get(color)
				.getDefaultState());
			world.setBlockAndUpdate(pos, newState);
			return InteractionResult.SUCCESS;
		}

		if (player instanceof FakePlayer)
			return InteractionResult.PASS;
		if (world.isClientSide)
			return InteractionResult.SUCCESS;

		withBlockEntityDo(world, pos,
			toolbox -> NetworkHooks.openScreen((ServerPlayer) player, toolbox, toolbox::sendToMenu));
		return InteractionResult.SUCCESS;
		
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState ifluidstate = context.getLevel()
			.getFluidState(context.getClickedPos());
		return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection()
			.getOpposite())
			.setValue(WATERLOGGED, Boolean.valueOf(ifluidstate.getType() == Fluids.WATER));
	}

	@Override
	public Class<ToolboxBlockEntity> getBlockEntityClass() {
		return ToolboxBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ToolboxBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.TOOLBOX.get();
	}

	public DyeColor getColor() {
		return color;
	}

	public static Ingredient getMainBox() {
		return Ingredient.of(AllBlocks.TOOLBOXES.get(DyeColor.BROWN)
			.get());
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		// TODO Auto-generated method stub
		return null;
	}

}