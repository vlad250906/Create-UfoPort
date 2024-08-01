package io.github.fabricators_of_create.porting_lib.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public interface CustomEnchantmentsItem {
	/**
	 * modify the enchantments found on the given stack.
	 * The map is a map of enchantments to the level found on the item. This map is mutable and may be directly modified.
	 */
	void modifyEnchantments(ItemEnchantments.Mutable enchantments, ItemStack stack);
}
