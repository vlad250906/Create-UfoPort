package com.simibubi.create.content.equipment.zapper;

import java.util.function.Function;
import java.util.function.Predicate;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ShootableGadgetItemMethods {

	public static void applyCooldown(Player player, ItemStack item, InteractionHand hand, Predicate<ItemStack> predicate,
		int cooldown) {
		if (cooldown <= 0)
			return;

		boolean gunInOtherHand =
			predicate.test(player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));
		player.getCooldowns()
			.addCooldown(item.getItem(), gunInOtherHand ? cooldown * 2 / 3 : cooldown);
	}

	public static void sendPackets(Player player, Function<Boolean, ? extends ShootGadgetPacket> factory) {
		if (!(player instanceof ServerPlayer))
			return;
		AllPackets.getChannel().sendToClientsTracking(factory.apply(false), player);
		AllPackets.getChannel().sendToClient(factory.apply(true), (ServerPlayer) player);
	}

	public static boolean shouldSwap(Player player, ItemStack item, InteractionHand hand, Predicate<ItemStack> predicate) {
		boolean isSwap = item.getOrDefault(AllDataComponents.ZAPPER, new CompoundTag())
			.contains("_Swap");
		boolean mainHand = hand == InteractionHand.MAIN_HAND;
		boolean gunInOtherHand = predicate.test(player.getItemInHand(mainHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));

		// Pass To Offhand
		if (mainHand && isSwap && gunInOtherHand)
			return true;
		if (mainHand && !isSwap && gunInOtherHand)
			ItemHelper.getOrCreateComponent(item, AllDataComponents.ZAPPER, new CompoundTag())
				.putBoolean("_Swap", true);
		if (!mainHand && isSwap)
			ItemHelper.getOrCreateComponent(item, AllDataComponents.ZAPPER, new CompoundTag())
				.remove("_Swap");
		if (!mainHand && gunInOtherHand)
			ItemHelper.getOrCreateComponent(player.getItemInHand(InteractionHand.MAIN_HAND), AllDataComponents.ZAPPER, new CompoundTag())
				.remove("_Swap");

		// (#574) fabric: on forge, this condition is patched into startUsingItem
		// skipping it causes an item to be used forever, only allowing 1 use before releasing and re-pressing the use button.
		if (item.getUseDuration(player) > 0) {
			player.startUsingItem(hand);
		}
		return false;
	}

	public static Vec3 getGunBarrelVec(Player player, boolean mainHand, Vec3 rightHandForward) {
		Vec3 start = player.position()
			.add(0, player.getEyeHeight(), 0);
		float yaw = (float) ((player.getYRot()) / -180 * Math.PI);
		float pitch = (float) ((player.getXRot()) / -180 * Math.PI);
		int flip = mainHand == (player.getMainArm() == HumanoidArm.RIGHT) ? -1 : 1;
		Vec3 barrelPosNoTransform = new Vec3(flip * rightHandForward.x, rightHandForward.y, rightHandForward.z);
		Vec3 barrelPos = start.add(barrelPosNoTransform.xRot(pitch)
			.yRot(yaw));
		return barrelPos;
	}

}
