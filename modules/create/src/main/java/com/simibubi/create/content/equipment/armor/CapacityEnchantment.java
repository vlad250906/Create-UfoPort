package com.simibubi.create.content.equipment.armor;

import io.github.fabricators_of_create.porting_lib.enchant.CustomEnchantingTableBehaviorEnchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public class CapacityEnchantment /*extends Enchantment implements CustomEnchantingTableBehaviorEnchantment*/ {

//	public CapacityEnchantment(EnchantmentDefinition definition) {
//		super(definition);
//	}
//
//	@Override
//	public boolean canApplyAtEnchantingTable(ItemStack stack) {
//		return stack.getItem() instanceof ICapacityEnchantable;
//	}
//
//	@Override
//	public boolean canEnchant(ItemStack stack) {
//		return canApplyAtEnchantingTable(stack);
//	}
//
	public interface ICapacityEnchantable {
	}

}
