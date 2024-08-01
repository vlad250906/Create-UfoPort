package com.simibubi.create.content.kinetics.mechanicalArm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ArmInteractionPointHandler {

	static List<ArmInteractionPoint> currentSelection = new ArrayList<>();
	static ItemStack currentItem;

	static long lastBlockPos = -1;

	public static InteractionResult rightClickingBlocksSelectsThem(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
		if (currentItem == null)
			return InteractionResult.PASS;
		BlockPos pos = hitResult.getBlockPos();//event.getPos();
//		Level world = event.getWorld();
		if (!world.isClientSide)
			return InteractionResult.PASS;
//		Player player = event.getPlayer();
		if (player != null && player.isSpectator())
			return InteractionResult.PASS;

		ArmInteractionPoint selected = getSelected(pos);
		BlockState state = world.getBlockState(pos);

		if (selected == null) {
			ArmInteractionPoint point = ArmInteractionPoint.create(world, pos, state);
			if (point == null)
				return InteractionResult.PASS;
			selected = point;
			put(point);
		}

		selected.cycleMode();
		if (player != null) {
			Mode mode = selected.getMode();
			Lang.builder()
				.translate(mode.getTranslationKey(), Lang.blockName(state)
					.style(ChatFormatting.WHITE))
				.color(mode.getColor())
				.sendStatus(player);
		}

//		event.setCanceled(true);
//		event.setCancellationResult(InteractionResult.SUCCESS);
		return InteractionResult.SUCCESS;
	}

	public static InteractionResult leftClickingBlocksDeselectsThem(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
		if (currentItem == null)
			return InteractionResult.PASS;
		if (!world.isClientSide)
			return InteractionResult.PASS;
		if (player.isSpectator())
			return InteractionResult.PASS;
//		BlockPos pos = event.getPos();
		if (remove(pos) != null) {
//			event.setCanceled(true);
//			event.setCancellationResult(InteractionResult.SUCCESS);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	public static void flushSettings(BlockPos pos) {
		if (currentSelection == null)
			return;

		int removed = 0;
		for (Iterator<ArmInteractionPoint> iterator = currentSelection.iterator(); iterator.hasNext();) {
			ArmInteractionPoint point = iterator.next();
			if (point.getPos()
				.closerThan(pos, ArmBlockEntity.getRange()))
				continue;
			iterator.remove();
			removed++;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (removed > 0) {
			Lang.builder()
				.translate("mechanical_arm.points_outside_range", removed)
				.style(ChatFormatting.RED)
				.sendStatus(player);
		} else {
			int inputs = 0;
			int outputs = 0;
			for (ArmInteractionPoint armInteractionPoint : currentSelection) {
				if (armInteractionPoint.getMode() == Mode.DEPOSIT)
					outputs++;
				else
					inputs++;
			}
			if (inputs + outputs > 0)
				Lang.builder()
					.translate("mechanical_arm.summary", inputs, outputs)
					.style(ChatFormatting.WHITE)
					.sendStatus(player);
		}

		AllPackets.getChannel().sendToServer(new ArmPlacementPacket(currentSelection, pos));
		currentSelection.clear();
		currentItem = null;
	}

	public static void tick() {
		Player player = Minecraft.getInstance().player;

		if (player == null)
			return;

		ItemStack heldItemMainhand = player.getMainHandItem();
		if (!AllBlocks.MECHANICAL_ARM.isIn(heldItemMainhand)) {
			currentItem = null;
		} else {
			if (heldItemMainhand != currentItem) {
				currentSelection.clear();
				currentItem = heldItemMainhand;
			}

			drawOutlines(currentSelection);
		}

		checkForWrench(heldItemMainhand);
	}

	private static void checkForWrench(ItemStack heldItem) {
		if (!AllItems.WRENCH.isIn(heldItem)) {
			return;
		}

		HitResult objectMouseOver = Minecraft.getInstance().hitResult;
		if (!(objectMouseOver instanceof BlockHitResult)) {
			return;
		}

		BlockHitResult result = (BlockHitResult) objectMouseOver;
		BlockPos pos = result.getBlockPos();

		BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
		if (!(be instanceof ArmBlockEntity)) {
			lastBlockPos = -1;
			currentSelection.clear();
			return;
		}

		if (lastBlockPos == -1 || lastBlockPos != pos.asLong()) {
			currentSelection.clear();
			ArmBlockEntity arm = (ArmBlockEntity) be;
			arm.inputs.forEach(ArmInteractionPointHandler::put);
			arm.outputs.forEach(ArmInteractionPointHandler::put);
			lastBlockPos = pos.asLong();
		}

		if (lastBlockPos != -1) {
			drawOutlines(currentSelection);
		}
	}

	private static void drawOutlines(Collection<ArmInteractionPoint> selection) {
		for (Iterator<ArmInteractionPoint> iterator = selection.iterator(); iterator.hasNext();) {
			ArmInteractionPoint point = iterator.next();

			if (!point.isValid()) {
				iterator.remove();
				continue;
			}

			Level level = point.getLevel();
			BlockPos pos = point.getPos();
			BlockState state = level.getBlockState(pos);
			VoxelShape shape = state.getShape(level, pos);
			if (shape.isEmpty())
				continue;

			int color = point.getMode()
				.getColor();
			CreateClient.OUTLINER.showAABB(point, shape.bounds()
				.move(pos))
				.colored(color)
				.lineWidth(1 / 16f);
		}
	}

	private static void put(ArmInteractionPoint point) {
		currentSelection.add(point);
	}

	private static ArmInteractionPoint remove(BlockPos pos) {
		ArmInteractionPoint result = getSelected(pos);
		if (result != null)
			currentSelection.remove(result);
		return result;
	}

	private static ArmInteractionPoint getSelected(BlockPos pos) {
		for (ArmInteractionPoint point : currentSelection)
			if (point.getPos()
				.equals(pos))
				return point;
		return null;
	}

}
