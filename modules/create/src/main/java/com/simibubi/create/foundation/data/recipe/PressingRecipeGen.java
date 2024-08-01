package com.simibubi.create.foundation.data.recipe;

import java.util.concurrent.CompletableFuture;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class PressingRecipeGen extends ProcessingRecipeGen {

	GeneratedRecipe

	SUGAR_CANE = create(() -> Items.SUGAR_CANE, b -> b.output(Items.PAPER)),

		PATH = create("path", b -> b.require(Ingredient.of(Items.GRASS_BLOCK, Items.DIRT))
			.output(Items.DIRT_PATH)),

		IRON = create("iron_ingot", b -> b.require(I.iron())
			.output(AllItems.IRON_SHEET.get())),
		GOLD = create("gold_ingot", b -> b.require(I.gold())
			.output(AllItems.GOLDEN_SHEET.get())),
		COPPER = create("copper_ingot", b -> b.require(I.copper())
			.output(AllItems.COPPER_SHEET.get())),
		BRASS = create("brass_ingot", b -> b.require(I.brass())
			.output(AllItems.BRASS_SHEET.get()))

	;

	public PressingRecipeGen(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	protected AllRecipeTypes getRecipeType() {
		return AllRecipeTypes.PRESSING;
	}

}
