package com.simibubi.create.foundation.data.recipe;

import java.util.concurrent.CompletableFuture;

import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags.AllFluidTags;

import io.github.fabricators_of_create.porting_lib.tags.Tags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

public class CompactingRecipeGen extends ProcessingRecipeGen {

	GeneratedRecipe

	GRANITE = create("granite_from_flint", b -> b.require(Items.FLINT)
		.require(Items.FLINT)
		.require(Fluids.LAVA, FluidConstants.BUCKET / 10)
		.require(Items.RED_SAND)
		.output(Blocks.GRANITE, 1)),

		DIORITE = create("diorite_from_flint", b -> b.require(Items.FLINT)
			.require(Items.FLINT)
			.require(Fluids.LAVA, FluidConstants.BUCKET / 10)
			.require(Items.CALCITE)
			.output(Blocks.DIORITE, 1)),

		ANDESITE = create("andesite_from_flint", b -> b.require(Items.FLINT)
			.require(Items.FLINT)
			.require(Fluids.LAVA, FluidConstants.BUCKET / 10)
			.require(Items.GRAVEL)
			.output(Blocks.ANDESITE, 1)),

		CHOCOLATE = create("chocolate", b -> b.require(AllFluids.CHOCOLATE.get(), FluidConstants.BOTTLE)
			.output(AllItems.BAR_OF_CHOCOLATE.get(), 1)),

		BLAZE_CAKE = create("blaze_cake", b -> b.require(Tags.Items.EGGS)
			.require(Items.SUGAR)
			.require(AllItems.CINDER_FLOUR.get())
			.output(AllItems.BLAZE_CAKE_BASE.get(), 1)),

		HONEY = create("honey", b -> b.require(AllFluidTags.HONEY.tag, FluidConstants.BUCKET)
			.output(Items.HONEY_BLOCK, 1))

	;

	public CompactingRecipeGen(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	protected AllRecipeTypes getRecipeType() {
		return AllRecipeTypes.COMPACTING;
	}

}
