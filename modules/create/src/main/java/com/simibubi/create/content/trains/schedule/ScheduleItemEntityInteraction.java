package com.simibubi.create.content.trains.schedule;

import com.simibubi.create.foundation.utility.AdventureUtil;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class ScheduleItemEntityInteraction {

	public static InteractionResult interactWithConductor(Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
		if (player == null || entity == null)
			return InteractionResult.PASS;
		if (player.isSpectator() || AdventureUtil.isAdventure(player))
			return InteractionResult.PASS;

		Entity rootVehicle = entity.getRootVehicle();
		if (!(rootVehicle instanceof CarriageContraptionEntity))
			return InteractionResult.PASS;
		if (!(entity instanceof LivingEntity living))
			return InteractionResult.PASS;
		if (player.getCooldowns()
			.isOnCooldown(AllItems.SCHEDULE.get()))
			return InteractionResult.PASS;

		ItemStack itemStack = player.getItemInHand(hand);
		if (itemStack.getItem()instanceof ScheduleItem si) {
			InteractionResult result = si.handScheduleTo(itemStack, player, living, hand);
			if (result.consumesAction()) {
				player.getCooldowns()
					.addCooldown(AllItems.SCHEDULE.get(), 5);
				return result;
			}
		}

		if (hand == InteractionHand.OFF_HAND)
			return InteractionResult.PASS;

		CarriageContraptionEntity cce = (CarriageContraptionEntity) rootVehicle;
		Contraption contraption = cce.getContraption();
		if (!(contraption instanceof CarriageContraption cc))
			return InteractionResult.PASS;

		Train train = cce.getCarriage().train;
		if (train == null)
			return InteractionResult.PASS;
		if (train.runtime.getSchedule() == null)
			return InteractionResult.PASS;

		Integer seatIndex = contraption.getSeatMapping()
			.get(entity.getUUID());
		if (seatIndex == null)
			return InteractionResult.PASS;
		BlockPos seatPos = contraption.getSeats()
			.get(seatIndex);
		Couple<Boolean> directions = cc.conductorSeats.get(seatPos);
		if (directions == null)
			return InteractionResult.PASS;

		boolean onServer = !world.isClientSide;

		if (train.runtime.paused && !train.runtime.completed) {
			if (onServer) {
				train.runtime.paused = false;
				AllSoundEvents.CONFIRM.playOnServer(player.level(), player.blockPosition(), 1, 1);
				player.displayClientMessage(Lang.translateDirect("schedule.continued"), true);
			}

			player.getCooldowns()
				.addCooldown(AllItems.SCHEDULE.get(), 5);
			return InteractionResult.SUCCESS;
		}

		ItemStack itemInHand = player.getItemInHand(hand);
		if (!itemInHand.isEmpty()) {
			if (onServer) {
				AllSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1, 1);
				player.displayClientMessage(Lang.translateDirect("schedule.remove_with_empty_hand"), true);
			}
			return InteractionResult.SUCCESS;
		}

		if (onServer) {
			AllSoundEvents.playItemPickup(player);
			player.displayClientMessage(
				Lang.translateDirect(
					train.runtime.isAutoSchedule ? "schedule.auto_removed_from_train" : "schedule.removed_from_train"),
				true);

			player.getInventory()
				.placeItemBackInInventory(train.runtime.returnSchedule());
		}

		player.getCooldowns()
			.addCooldown(AllItems.SCHEDULE.get(), 5);
		return InteractionResult.SUCCESS;
	}

}
