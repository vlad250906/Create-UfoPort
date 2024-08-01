package com.simibubi.create.foundation.data.recipe;

import java.util.concurrent.CompletableFuture;

import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.recipe.BlockTagIngredient;

import io.github.fabricators_of_create.porting_lib.tags.Tags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

public class MixingRecipeGen extends ProcessingRecipeGen {

	GeneratedRecipe

	TEMP_LAVA = create("lava_from_cobble", b -> b.require(Tags.Items.COBBLESTONES)
		.output(Fluids.LAVA, FluidConstants.BUCKET / 20)
		.requiresHeat(HeatCondition.SUPERHEATED)),

		TEA = create("tea", b -> b.require(Fluids.WATER, FluidConstants.BOTTLE)
			.require(Tags.Fluids.MILK, FluidConstants.BOTTLE)
			.require(ItemTags.LEAVES)
			.output(AllFluids.TEA.get(), FluidConstants.BOTTLE * 2)
			.requiresHeat(HeatCondition.HEATED)),

		CHOCOLATE = create("chocolate", b -> b.require(Tags.Fluids.MILK, FluidConstants.BOTTLE)
			.require(Items.SUGAR)
			.require(Items.COCOA_BEANS)
			.output(AllFluids.CHOCOLATE.get(), FluidConstants.BOTTLE)
			.requiresHeat(HeatCondition.HEATED)),

		CHOCOLATE_MELTING = create("chocolate_melting", b -> b.require(AllItems.BAR_OF_CHOCOLATE.get())
			.output(AllFluids.CHOCOLATE.get(), FluidConstants.BOTTLE)
			.requiresHeat(HeatCondition.HEATED)),

		HONEY = create("honey", b -> b.require(Items.HONEY_BLOCK)
			.output(AllFluids.HONEY.get(), FluidConstants.BUCKET)
			.requiresHeat(HeatCondition.HEATED)),

		DOUGH = create("dough_by_mixing", b -> b.require(I.wheatFlour())
			.require(Fluids.WATER, FluidConstants.BUCKET)
			.output(AllItems.DOUGH.get(), 1)),

		BRASS_INGOT = create("brass_ingot", b -> b.require(I.copper())
			.require(I.zinc())
			.output(AllItems.BRASS_INGOT.get(), 2)
			.requiresHeat(HeatCondition.HEATED)),

		ANDESITE_ALLOY = create("andesite_alloy", b -> b.require(Blocks.ANDESITE)
			.require(I.ironNugget())
			.output(I.andesite(), 1)),

		ANDESITE_ALLOY_FROM_ZINC = create("andesite_alloy_from_zinc", b -> b.require(Blocks.ANDESITE)
			.require(I.zincNugget())
			.output(I.andesite(), 1)),

		MUD = create("mud_by_mixing", b -> b.require(BlockTagIngredient.create(BlockTags.CONVERTABLE_TO_MUD))
			.require(Fluids.WATER, FluidConstants.BOTTLE)
			.output(Blocks.MUD, 1))

	;

	public MixingRecipeGen(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	protected AllRecipeTypes getRecipeType() {
		return AllRecipeTypes.MIXING;
	}

}
