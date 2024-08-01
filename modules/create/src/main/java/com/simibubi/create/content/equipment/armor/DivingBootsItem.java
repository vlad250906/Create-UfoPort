package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.foundation.utility.NBTHelper;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.LivingEntityAccessor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class DivingBootsItem extends BaseArmorItem {
	public static final EquipmentSlot SLOT = EquipmentSlot.FEET;
	public static final ArmorItem.Type TYPE = ArmorItem.Type.BOOTS;

	public DivingBootsItem(Holder<ArmorMaterial> material, Properties properties, ResourceLocation textureLoc) {
		super(material, TYPE, properties, textureLoc);
	}

	public static boolean isWornBy(Entity entity) {
		return !getWornItem(entity).isEmpty();
	}

	public static ItemStack getWornItem(Entity entity) {
		if (!(entity instanceof LivingEntity livingEntity)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = livingEntity.getItemBySlot(SLOT);
		if (!(stack.getItem() instanceof DivingBootsItem)) {
			return ItemStack.EMPTY;
		}
		return stack;
	}

	public static void accellerateDescentUnderwater(LivingEntity entity) {
//		LivingEntity entity = event.getEntityLiving();
		if (!affects(entity))
			return;

		Vec3 motion = entity.getDeltaMovement();
		boolean isJumping = ((LivingEntityAccessor) entity).port_lib$isJumping();
		entity.setOnGround(entity.onGround() || entity.verticalCollision);

		if (isJumping && entity.onGround()) {
			motion = motion.add(0, .5f, 0);
			entity.setOnGround(false);
		} else {
			motion = motion.add(0, -0.05f, 0);
		}

		float multiplier = 1.3f;
		if (motion.multiply(1, 0, 1)
			.length() < 0.145f && (entity.zza > 0 || entity.xxa != 0) && !entity.isShiftKeyDown())
			motion = motion.multiply(multiplier, 1, multiplier);
		entity.setDeltaMovement(motion);
	}

	protected static boolean affects(LivingEntity entity) {
		//TODO
		if (!isWornBy(entity)) {
			entity.getCustomData()
				.remove("HeavyBoots");
			return false;
		}

		NBTHelper.putMarker(entity.getCustomData(), "HeavyBoots");
		if (!entity.isInWater())
			return false;
		if (entity.getPose() == Pose.SWIMMING)
			return false;
		if (entity instanceof Player) {
			Player playerEntity = (Player) entity;
			if (playerEntity.getAbilities().flying)
				return false;
		}
		return true;
	}

	public static Vec3 getMovementMultiplier(LivingEntity entity) {
		double yMotion = entity.getDeltaMovement().y;
		double vMultiplier = yMotion < 0 ? Math.max(0, 2.5 - Math.abs(yMotion) * 2) : 1;

		if (!entity.onGround()) {
			if (((LivingEntityAccessor) entity).port_lib$isJumping() && entity.getCustomData()
				.contains("LavaGrounded")) 
			{
				boolean eyeInFluid = entity.isEyeInFluid(FluidTags.LAVA);
				vMultiplier = yMotion == 0 ? 0 : (eyeInFluid ? 1 : 0.5) / yMotion;
			} else if (yMotion > 0)
				vMultiplier = 1.3;

			entity.getCustomData()
				.remove("LavaGrounded");
			return new Vec3(1.75, vMultiplier, 1.75);
		}

		entity.getCustomData()
			.putBoolean("LavaGrounded", true);
		double hMultiplier = entity.isSprinting() ? 1.85 : 1.75;
		return new Vec3(hMultiplier, vMultiplier, hMultiplier);
	}

}
