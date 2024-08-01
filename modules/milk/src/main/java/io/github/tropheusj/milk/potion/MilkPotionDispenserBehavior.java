package io.github.tropheusj.milk.potion;

import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.world.item.ItemStack;

public enum MilkPotionDispenserBehavior implements DispenseItemBehavior {
	INSTANCE;

	@Override
	public ItemStack dispense(BlockSource blockPointer, ItemStack itemStack) {
		return new ProjectileDispenseBehavior(itemStack.getItem()).dispense(blockPointer, itemStack);
	}
}
