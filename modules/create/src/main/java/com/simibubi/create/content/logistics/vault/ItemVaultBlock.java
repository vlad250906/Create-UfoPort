package com.simibubi.create.content.logistics.vault;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;

import io.github.fabricators_of_create.porting_lib.block.CustomSoundTypeBlock;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class ItemVaultBlock extends Block implements IWrenchable, IBE<ItemVaultBlockEntity>, CustomSoundTypeBlock {

	public static final Property<Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	public static final BooleanProperty LARGE = BooleanProperty.create("large");

	public ItemVaultBlock(Properties p_i48440_1_) {
		super(p_i48440_1_);
		registerDefaultState(defaultBlockState().setValue(LARGE, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		pBuilder.add(HORIZONTAL_AXIS, LARGE);
		super.createBlockStateDefinition(pBuilder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		if (pContext.getPlayer() == null || !pContext.getPlayer()
			.isShiftKeyDown()) {
			BlockState placedOn = pContext.getLevel()
				.getBlockState(pContext.getClickedPos()
					.relative(pContext.getClickedFace()
						.getOpposite()));
			Axis preferredAxis = getVaultBlockAxis(placedOn);
			if (preferredAxis != null)
				return this.defaultBlockState()
					.setValue(HORIZONTAL_AXIS, preferredAxis);
		}
		return this.defaultBlockState()
			.setValue(HORIZONTAL_AXIS, pContext.getHorizontalDirection()
				.getAxis());
	}

	@Override
	public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
		if (pOldState.getBlock() == pState.getBlock())
			return;
		if (pIsMoving)
			return;
		// fabric: see comment in FluidTankItem
		Consumer<ItemVaultBlockEntity> consumer = ItemVaultItem.IS_PLACING_NBT
				? ItemVaultBlockEntity::queueConnectivityUpdate
				: ItemVaultBlockEntity::updateConnectivity;
		withBlockEntityDo(pLevel, pPos, consumer);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (context.getClickedFace()
			.getAxis()
			.isVertical()) {
			BlockEntity be = context.getLevel()
				.getBlockEntity(context.getClickedPos());
			if (be instanceof ItemVaultBlockEntity) {
				ItemVaultBlockEntity vault = (ItemVaultBlockEntity) be;
				ConnectivityHandler.splitMulti(vault);
				vault.removeController(true);
			}
			state = state.setValue(LARGE, false);
		}
		InteractionResult onWrenched = IWrenchable.super.onWrenched(state, context);
		return onWrenched;
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean pIsMoving) {
		if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
			BlockEntity be = world.getBlockEntity(pos);
			if (!(be instanceof ItemVaultBlockEntity))
				return;
			ItemVaultBlockEntity vaultBE = (ItemVaultBlockEntity) be;
			ItemHelper.dropContents(world, pos, vaultBE.inventory);
			world.removeBlockEntity(pos);
			ConnectivityHandler.splitMulti(vaultBE);
		}
	}

	public static boolean isVault(BlockState state) {
		return AllBlocks.ITEM_VAULT.has(state);
	}

	@Nullable
	public static Axis getVaultBlockAxis(BlockState state) {
		if (!isVault(state))
			return null;
		return state.getValue(HORIZONTAL_AXIS);
	}

	public static boolean isLarge(BlockState state) {
		if (!isVault(state))
			return false;
		return state.getValue(LARGE);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		Axis axis = state.getValue(HORIZONTAL_AXIS);
		return state.setValue(HORIZONTAL_AXIS, rot.rotate(Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE))
			.getAxis());
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state;
	}

	// Vaults are less noisy when placed in batch
	public static final SoundType SILENCED_METAL =
		new SoundType(0.1F, 1.5F, SoundEvents.NETHERITE_BLOCK_BREAK, SoundEvents.NETHERITE_BLOCK_STEP,
			SoundEvents.NETHERITE_BLOCK_PLACE, SoundEvents.NETHERITE_BLOCK_HIT,
			SoundEvents.NETHERITE_BLOCK_FALL);

	@Override
	public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, Entity entity) {
		SoundType soundType = getSoundType(state);
		if (entity != null && entity.getCustomData()
			.contains("SilenceVaultSound"))
			return SILENCED_METAL;
		return soundType;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState p_149740_1_) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
		return getBlockEntityOptional(pLevel, pPos)
			.filter(vte -> !Transaction.isOpen()) // fabric: hack fix for comparators updating when they shouldn't
			.map(vte -> vte.getItemStorage(null))
			.map(ItemHelper::calcRedstoneFromInventory)
			.orElse(0);
	}

	@Override
	public BlockEntityType<? extends ItemVaultBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ITEM_VAULT.get();
	}

	@Override
	public Class<ItemVaultBlockEntity> getBlockEntityClass() {
		return ItemVaultBlockEntity.class;
	}
}
