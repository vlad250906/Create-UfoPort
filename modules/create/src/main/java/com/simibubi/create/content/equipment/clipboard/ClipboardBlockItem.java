package com.simibubi.create.content.equipment.clipboard;

import javax.annotation.Nonnull;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class ClipboardBlockItem extends BlockItem {

	public ClipboardBlockItem(Block pBlock, Properties pProperties) {
		super(pBlock, pProperties);
	}

	@Nonnull
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null)
			return InteractionResult.PASS;
		if (player.isShiftKeyDown())
			return super.useOn(context);
		return use(context.getLevel(), player, context.getHand()).getResult();
	}

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pPos, Level pLevel, Player pPlayer, ItemStack pStack,
		BlockState pState) {
		if (pLevel.isClientSide())
			return false;
		if (!(pLevel.getBlockEntity(pPos) instanceof ClipboardBlockEntity cbe))
			return false;
		cbe.dataContainer = ItemHandlerHelper.copyStackWithSize(pStack, 1);
		cbe.notifyUpdate();
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack heldItem = player.getItemInHand(hand);
		if (hand == InteractionHand.OFF_HAND)
			return InteractionResultHolder.pass(heldItem);

		player.getCooldowns()
			.addCooldown(heldItem.getItem(), 10);
		if (world.isClientSide)
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> openScreen(player, heldItem));
		CompoundTag tag = ItemHelper.getOrCreateComponent(heldItem, AllDataComponents.CLIPBOARD_EDITING, new CompoundTag());
		tag.putInt("Type", ClipboardOverrides.ClipboardType.EDITING.ordinal());

		return InteractionResultHolder.success(heldItem);
	}

	@Environment(EnvType.CLIENT)
	private void openScreen(Player player, ItemStack stack) {
		if (Minecraft.getInstance().player == player)
			ScreenOpener.open(new ClipboardScreen(player.getInventory().selected, stack, null));
	}

	public void registerModelOverrides() {
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> ClipboardOverrides.registerModelOverridesClient(this));
	}

}
