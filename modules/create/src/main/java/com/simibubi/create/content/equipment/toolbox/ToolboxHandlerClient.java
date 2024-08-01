package com.simibubi.create.content.equipment.toolbox;

import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_HOTBAR_OFF;
import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_HOTBAR_ON;
import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_SELECTED_OFF;
import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_SELECTED_ON;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.NbtFixer;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.fabricmc.fabric.api.entity.EntityPickInteractionAware;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ToolboxHandlerClient {

	static int COOLDOWN = 0;

	public static void clientTick() {
		if (COOLDOWN > 0 && !AllKeys.TOOLBELT.isPressed())
			COOLDOWN--;
	}

	public static boolean onPickItem() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null)
			return false;
		Level level = player.level();
		HitResult hitResult = mc.hitResult;

		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS)
			return false;
		if (player.isCreative())
			return false;

		ItemStack result = ItemStack.EMPTY;
		List<ToolboxBlockEntity> toolboxes = ToolboxHandler.getNearest(player.level(), player, 8);

		if (toolboxes.isEmpty())
			return false;

		if (hitResult.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (state.isAir())
				return false;
			Block block = state.getBlock();
			result = block instanceof BlockPickInteractionAware aware
					? aware.getPickedStack(state, level, pos, player, hitResult)
					: block.getCloneItemStack(level, pos, state);
		} else if (hitResult.getType() == HitResult.Type.ENTITY) {
			Entity entity = ((EntityHitResult) hitResult).getEntity();
			result = entity instanceof EntityPickInteractionAware aware
					? aware.getPickedStack(player, hitResult)
					: entity.getPickResult();
		}

		if (result == null || result.isEmpty())
			return false;

		for (ToolboxBlockEntity toolboxBlockEntity : toolboxes) {
			ToolboxInventory inventory = toolboxBlockEntity.inventory;
			try (Transaction t = TransferUtil.getTransaction()) {
				for (int comp = 0; comp < 8; comp++) {
					ItemStack inSlot = inventory.takeFromCompartment(1, comp, t);
					if (inSlot.isEmpty())
						continue;
					if (inSlot.getItem() != result.getItem())
						continue;
					if (!ItemStack.matches(inSlot, result))
						continue;

					AllPackets.getChannel().sendToServer(
							new ToolboxEquipPacket(toolboxBlockEntity.getBlockPos(), comp, player.getInventory().selected));
					return true;
				}
			}
		}

		return false;
	}

	public static void onKeyInput(int key, boolean pressed) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
			return;

		if (key != AllKeys.TOOLBELT.getBoundCode() || !pressed)
			return;
		if (COOLDOWN > 0)
			return;
		LocalPlayer player = mc.player;
		if (player == null)
			return;
		Level level = player.level();

		List<ToolboxBlockEntity> toolboxes = ToolboxHandler.getNearest(player.level(), player, 8);
		toolboxes.sort(Comparator.comparing(ToolboxBlockEntity::getUniqueId));

		CompoundTag compound = player.getCustomData()
			.getCompound("CreateToolboxData");

		String slotKey = String.valueOf(player.getInventory().selected);
		boolean equipped = compound.contains(slotKey);

		if (equipped) {
			BlockPos pos = NbtFixer.readBlockPos(compound.getCompound(slotKey), "Pos");
			double max = ToolboxHandler.getMaxRange(player);
			boolean canReachToolbox = ToolboxHandler.distance(player.position(), pos) < max * max;

			if (canReachToolbox) {
				BlockEntity blockEntity = level.getBlockEntity(pos);
				if (blockEntity instanceof ToolboxBlockEntity) {
					RadialToolboxMenu screen = new RadialToolboxMenu(toolboxes,
						RadialToolboxMenu.State.SELECT_ITEM_UNEQUIP, (ToolboxBlockEntity) blockEntity);
					screen.prevSlot(compound.getCompound(slotKey)
						.getInt("Slot"));
					ScreenOpener.open(screen);
					return;
				}
			}

			ScreenOpener.open(new RadialToolboxMenu(ImmutableList.of(), RadialToolboxMenu.State.DETACH, null));
			return;
		}

		if (toolboxes.isEmpty())
			return;

		if (toolboxes.size() == 1)
			ScreenOpener.open(new RadialToolboxMenu(toolboxes, RadialToolboxMenu.State.SELECT_ITEM, toolboxes.get(0)));
		else
			ScreenOpener.open(new RadialToolboxMenu(toolboxes, RadialToolboxMenu.State.SELECT_BOX, null));
	}

	public static void renderOverlay(GuiGraphics graphics, float partialTicks, Window window) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
			return;

		int x = window.getGuiScaledWidth() / 2 - 90;
		int y = window.getGuiScaledHeight() - 23;
		RenderSystem.enableDepthTest();

		Player player = mc.player;
		CompoundTag persistentData = player.getCustomData();
		if (!persistentData.contains("CreateToolboxData"))
			return;

		CompoundTag compound = player.getCustomData()
			.getCompound("CreateToolboxData");

		if (compound.isEmpty())
			return;

		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();
		for (int slot = 0; slot < 9; slot++) {
			String key = String.valueOf(slot);
			if (!compound.contains(key))
				continue;
			BlockPos pos = NbtFixer.readBlockPos(compound.getCompound(key), "Pos");
			double max = ToolboxHandler.getMaxRange(player);
			boolean selected = player.getInventory().selected == slot;
			int offset = selected ? 1 : 0;
			AllGuiTextures texture = ToolboxHandler.distance(player.position(), pos) < max * max
				? selected ? TOOLBELT_SELECTED_ON : TOOLBELT_HOTBAR_ON
				: selected ? TOOLBELT_SELECTED_OFF : TOOLBELT_HOTBAR_OFF;
			texture.render(graphics, x + 20 * slot - offset, y + offset - AllConfigs.client().toolboxHotbarOverlayOffset.get());
		}
		poseStack.popPose();
	}

}
