package com.simibubi.create.content.equipment.extendoGrip;

import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public class ExtendoGripItem extends Item  {
	public static final int MAX_DAMAGE = 200;

	public static final AttributeModifier singleRangeAttributeModifier =
		new AttributeModifier(ResourceLocation.parse("create:single_range_modifier")/*UUID.fromString("7f7dbdb2-0d0d-458a-aa40-ac7633691f66"), "Range modifier"*/, 3,
			AttributeModifier.Operation.ADD_VALUE);
	public static final AttributeModifier doubleRangeAttributeModifier =
		new AttributeModifier(ResourceLocation.parse("create:double_range_modifier")/*UUID.fromString("8f7dbdb2-0d0d-458a-aa40-ac7633691f66"), "Range modifier"*/, 5,
			AttributeModifier.Operation.ADD_VALUE);

	private static final Supplier<Multimap<Holder<Attribute>, AttributeModifier>> rangeModifier = Suppliers.memoize(() ->
	// Holding an ExtendoGrip
		ImmutableMultimap.of(Attributes.BLOCK_INTERACTION_RANGE, singleRangeAttributeModifier, Attributes.ENTITY_INTERACTION_RANGE, singleRangeAttributeModifier));
	private static final Supplier<Multimap<Holder<Attribute>, AttributeModifier>> doubleRangeModifier = Suppliers.memoize(() ->
	// Holding two ExtendoGrips o.O
		ImmutableMultimap.of(Attributes.BLOCK_INTERACTION_RANGE, doubleRangeAttributeModifier, Attributes.ENTITY_INTERACTION_RANGE, doubleRangeAttributeModifier));

	private static DamageSource lastActiveDamageSource;

	public ExtendoGripItem(Properties properties) {
		super(properties.durability(MAX_DAMAGE));
	}

	public static final String EXTENDO_MARKER = "createExtendo";
	public static final String DUAL_EXTENDO_MARKER = "createDualExtendo";

	public static void holdingExtendoGripIncreasesRange(LivingEntity entity) {
		if (!(entity instanceof Player))
			return;

		Player player = (Player) entity;

		CompoundTag persistentData = player.getCustomData();
		boolean inOff = AllItems.EXTENDO_GRIP.isIn(player.getOffhandItem());
		boolean inMain = AllItems.EXTENDO_GRIP.isIn(player.getMainHandItem());
		boolean holdingDualExtendo = inOff && inMain;
		boolean holdingExtendo = inOff ^ inMain;
		holdingExtendo &= !holdingDualExtendo;
		boolean wasHoldingExtendo = persistentData.contains(EXTENDO_MARKER);
		boolean wasHoldingDualExtendo = persistentData.contains(DUAL_EXTENDO_MARKER);

		if (holdingExtendo != wasHoldingExtendo) {
			if (!holdingExtendo) {
				removeAttributeModifiers(player.getAttributes(), rangeModifier.get());
				//player.getAttributes()
					//.removeAttributeModifiers(rangeModifier.get());
				persistentData.remove(EXTENDO_MARKER);
			} else {
				AllAdvancements.EXTENDO_GRIP.awardTo(player);
				addTransientAttributeModifiers(player.getAttributes(), rangeModifier.get());
				//player.getAttributes()
					//.addTransientAttributeModifiers(rangeModifier.get());
				persistentData.putBoolean(EXTENDO_MARKER, true);
			}
		}

		if (holdingDualExtendo != wasHoldingDualExtendo) {
			if (!holdingDualExtendo) {
				removeAttributeModifiers(player.getAttributes(), doubleRangeModifier.get());
				//player.getAttributes()
					//.removeAttributeModifiers(doubleRangeModifier.get());
				persistentData.remove(DUAL_EXTENDO_MARKER);
			} else {
				AllAdvancements.EXTENDO_GRIP_DUAL.awardTo(player);
				addTransientAttributeModifiers(player.getAttributes(), doubleRangeModifier.get());
				//player.getAttributes()
					//.addTransientAttributeModifiers(doubleRangeModifier.get());
				persistentData.putBoolean(DUAL_EXTENDO_MARKER, true);
			}
		}

	}
	
	public static void removeAttributeModifiers(AttributeMap mapat, Multimap<Holder<Attribute>, AttributeModifier> map) {
	        map.asMap().forEach((attribute, collection) -> {
	            AttributeInstance attributeInstance = mapat.getInstance(attribute);
	            if (attributeInstance != null) {
	                collection.forEach(attributeModifier -> attributeInstance.removeModifier(attributeModifier));
	            }
	        });
	}
	
	public static void addTransientAttributeModifiers(AttributeMap mapat, Multimap<Holder<Attribute>, AttributeModifier> map) {
        map.forEach((attribute, attributeModifier) -> {
            AttributeInstance attributeInstance = mapat.getInstance(attribute);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(attributeModifier);
                attributeInstance.addTransientModifier((AttributeModifier)attributeModifier);
            }
        });
    }

	public static void addReachToJoiningPlayersHoldingExtendo(Entity entity, @Nullable CompoundTag persistentData) {
		if (!(entity instanceof Player player) || persistentData == null) return;
//		Player player = event.getPlayer();
//		CompoundTag persistentData = player.getCustomData();

		if (persistentData.contains(DUAL_EXTENDO_MARKER))
			addTransientAttributeModifiers(player.getAttributes(), doubleRangeModifier.get());
			//player.getAttributes()
				//.addTransientAttributeModifiers(doubleRangeModifier.get());
		else if (persistentData.contains(EXTENDO_MARKER))
			addTransientAttributeModifiers(player.getAttributes(), rangeModifier.get());
			//player.getAttributes()
				//.addTransientAttributeModifiers(rangeModifier.get());
	}

	private static void findAndDamageExtendoGrip(Player player) {
		if (player == null)
			return;
		if (player.level().isClientSide)
			return;
		InteractionHand hand = InteractionHand.MAIN_HAND;
		ItemStack extendo = player.getMainHandItem();
		if (!AllItems.EXTENDO_GRIP.isIn(extendo)) {
			extendo = player.getOffhandItem();
			hand = InteractionHand.OFF_HAND;
		}
		if (!AllItems.EXTENDO_GRIP.isIn(extendo))
			return;
		final InteractionHand h = hand;
		if (!BacktankUtil.canAbsorbDamage(player, maxUses()))
			extendo.hurtAndBreak(1, player, h == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return BacktankUtil.isBarVisible(stack, maxUses());
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return BacktankUtil.getBarWidth(stack, maxUses());
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return BacktankUtil.getBarColor(stack, maxUses());
	}

	private static int maxUses() {
		return AllConfigs.server().equipment.maxExtendoGripActions.get();
	}

	public static float bufferLivingAttackEvent(DamageSource damageSource, LivingEntity attacked, float amount) {
		// Workaround for removed patch to get the attacking entity.
		lastActiveDamageSource = damageSource;

		Entity trueSource = damageSource.getEntity();
		if (trueSource instanceof Player)
			findAndDamageExtendoGrip((Player) trueSource);
		return amount;
	}

	public static double attacksByExtendoGripHaveMoreKnockback(double strength, Player player) {
		if (!isHoldingExtendoGrip(player))
			return strength;
		return strength + 2;
	}

	public static boolean isHoldingExtendoGrip(Player player) {
		boolean inOff = AllItems.EXTENDO_GRIP.isIn(player.getOffhandItem());
		boolean inMain = AllItems.EXTENDO_GRIP.isIn(player.getMainHandItem());
		boolean holdingGrip = inOff || inMain;
		return holdingGrip;
	}
}
