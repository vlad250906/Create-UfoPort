package com.simibubi.create;

import static com.simibubi.create.Create.REGISTRATE;

import com.simibubi.create.content.equipment.armor.CapacityEnchantment;
import com.simibubi.create.content.equipment.potatoCannon.PotatoRecoveryEnchantment;
import com.tterrag.registrate.builders.EnchantmentBuilder.EnchantmentRarity;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;

public class AllEnchantments {
	
	public static final ResourceKey<Enchantment> POTATO_RECOVERY = ResourceKey.create(
			Registries.ENCHANTMENT.location(), ResourceLocation.fromNamespaceAndPath("create", "potato_recovery"));
	
	public static final ResourceKey<Enchantment> CAPACITY = ResourceKey.create(
			Registries.ENCHANTMENT.location(), ResourceLocation.fromNamespaceAndPath("create", "capacity"));

//	public static final RegistryEntry<Enchantment> POTATO_RECOVERY = REGISTRATE.object("potato_recovery")
//		.enchantment(ItemTags.BOW_ENCHANTABLE., def -> new Enchantment(Component.translatable("potato_recovery"), def, HolderSet.empty(), DataComponentMap.EMPTY))
//		.addSlots(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND)
//		.lang("Potato Recovery")
//		.weight(EnchantmentRarity.UNCOMMON)
//		.maxLevel(3)
//		.register();
//	
//	public static final RegistryEntry<Enchantment> CAPACITY = REGISTRATE.object("capacity")
//		.enchantment(ItemTags.CHEST_ARMOR_ENCHANTABLE, CapacityEnchantment::new)
//		.addSlots(EquipmentSlot.CHEST)
//		.lang("Capacity")
//		.weight(EnchantmentRarity.COMMON)
//		.maxLevel(3)
//		.register();

	public static void register() {}

}
