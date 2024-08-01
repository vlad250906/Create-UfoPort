package com.simibubi.create.foundation.item;

import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public class CombustibleItem extends Item {
	private int burnTime = -1;

	public CombustibleItem(Properties properties) {
		super(properties);
	}

	public void setBurnTime(int burnTime) {
		FuelRegistry.INSTANCE.add(this, burnTime);
		this.burnTime = burnTime;
	}

//	@Override
	public int getBurnTime(ItemStack itemStack, RecipeType<?> recipeType) {
		return this.burnTime;
	}

}
