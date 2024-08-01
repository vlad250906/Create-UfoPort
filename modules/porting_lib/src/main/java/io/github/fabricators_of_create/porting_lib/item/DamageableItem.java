package io.github.fabricators_of_create.porting_lib.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface DamageableItem {
	private Item self() {
		return (Item) this;
	}

	/**
	 * Return the itemDamage represented by this ItemStack. Defaults to the Damage
	 * entry in the stack NBT, but can be overridden here for other sources.
	 *
	 * @param stack The itemstack that is damaged
	 * @return the damage value
	 */
	default int getDamage(ItemStack stack) {
		return stack.has(DataComponents.DAMAGE) ? 0 : stack.get(DataComponents.DAMAGE);
	}

	/**
	 * Return the maxDamage for this ItemStack. Defaults to the maxDamage field in
	 * this item, but can be overridden here for other sources such as NBT.
	 *
	 * @param stack The itemstack that is damaged
	 * @return the damage value
	 */
	@SuppressWarnings("deprecation")
	default int getMaxDamage(ItemStack stack) {
		return stack.get(DataComponents.MAX_DAMAGE);
	}

	/**
	 * Set the damage for this itemstack. Note, this method is responsible for zero
	 * checking.
	 *
	 * @param stack  the stack
	 * @param damage the new damage value
	 */
	default void setDamage(ItemStack stack, int damage) {
		stack.set(DataComponents.DAMAGE, Math.max(0, damage));
	}
}
