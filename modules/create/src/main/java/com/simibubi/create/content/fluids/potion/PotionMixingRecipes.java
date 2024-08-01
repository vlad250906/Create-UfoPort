package com.simibubi.create.content.fluids.potion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.simibubi.create.Create;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import io.github.fabricators_of_create.porting_lib.brewing.BrewingRecipe;
import io.github.fabricators_of_create.porting_lib.brewing.BrewingRecipeRegistry;
import io.github.fabricators_of_create.porting_lib.brewing.IBrewingRecipe;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.PotionBrewing$MixAccessor;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.PotionBrewingAccessor;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;

public class PotionMixingRecipes {

	public static final List<Item> SUPPORTED_CONTAINERS = List.of(Items.POTION, Items.SPLASH_POTION,
			Items.LINGERING_POTION);

	public static final List<MixingRecipe> ALL = createRecipes();
	public static final Map<Item, List<MixingRecipe>> BY_ITEM = sortRecipesByItem(ALL);
	
	private static boolean isContainer(List<Ingredient> containers, ItemStack stack) {
		for (Ingredient ingredient : containers) {
			if (!ingredient.test(stack)) continue;
				return true;
	    }
	    return false;
	}

	private static List<MixingRecipe> createRecipes() {
		List<MixingRecipe> mixingRecipes = new ArrayList<>();
		PotionBrewingAccessor pba = (PotionBrewingAccessor)Create.getPotionBrewing();

		int recipeIndex = 0;

		List<Item> allowedSupportedContainers = new ArrayList<>();
		List<ItemStack> supportedContainerStacks = new ArrayList<>();
		for (Item container : SUPPORTED_CONTAINERS) {
			ItemStack stack = new ItemStack(container);
			supportedContainerStacks.add(stack);
			if (isContainer(pba.port_lib$ALLOWED_CONTAINERS(), stack)) {
				allowedSupportedContainers.add(container);
			}
		}

		for (Item container : allowedSupportedContainers) {
			BottleType bottleType = PotionFluidHandler.bottleTypeFromItem(container);
			for (PotionBrewing.Mix<Potion> mix : pba.port_lib$POTION_MIXES()) {
				FluidStack fromFluid = PotionFluidHandler.getFluidFromPotion(mix.from(), bottleType,
						FluidConstants.BUCKET);
				FluidStack toFluid = PotionFluidHandler.getFluidFromPotion(mix.to(), bottleType,
						FluidConstants.BUCKET);

				mixingRecipes.add(createRecipe("potion_mixing_vanilla_" + recipeIndex++, mix.ingredient(),
						fromFluid, toFluid));
			}
		}

		for (PotionBrewing.Mix<Item> mix : pba.port_lib$CONTAINER_MIXES()) {
			Item from = mix.from().value();
			if (!allowedSupportedContainers.contains(from)) {
				continue;
			}
			Item to = mix.to().value();
			if (!allowedSupportedContainers.contains(to)) {
				continue;
			}
			BottleType fromBottleType = PotionFluidHandler.bottleTypeFromItem(from);
			BottleType toBottleType = PotionFluidHandler.bottleTypeFromItem(to);
			Ingredient ingredient = mix.ingredient();

			for (Entry<ResourceKey<Potion>, Potion> entry : BuiltInRegistries.POTION.entrySet()) {
				Holder<Potion> potion = BuiltInRegistries.POTION.wrapAsHolder(entry.getValue());
				if (potion == null) {
					continue;
				}

				FluidStack fromFluid = PotionFluidHandler.getFluidFromPotion(potion, fromBottleType,
						FluidConstants.BUCKET);
				FluidStack toFluid = PotionFluidHandler.getFluidFromPotion(potion, toBottleType, FluidConstants.BUCKET);

				mixingRecipes
						.add(createRecipe("potion_mixing_vanilla_" + recipeIndex++, ingredient, fromFluid, toFluid));
			}
		}

		recipeIndex = 0;
		for (IBrewingRecipe recipe : BrewingRecipeRegistry.getRecipes()) {
			if (recipe instanceof BrewingRecipe recipeImpl) {
				ItemStack output = recipeImpl.getOutput();
				if (!SUPPORTED_CONTAINERS.contains(output.getItem())) {
					continue;
				}

				Ingredient input = recipeImpl.getInput();
				Ingredient ingredient = recipeImpl.getIngredient();
				FluidStack outputFluid = null;
				for (ItemStack stack : supportedContainerStacks) {
					if (input.test(stack)) {
						FluidStack inputFluid = PotionFluidHandler.getFluidFromPotionItem(stack);
						if (outputFluid == null) {
							outputFluid = PotionFluidHandler.getFluidFromPotionItem(output);
						}
						mixingRecipes.add(createRecipe("potion_mixing_modded_" + recipeIndex++, ingredient, inputFluid,
								outputFluid));
					}
				}
			}
		}

		return mixingRecipes;
	}

	private static MixingRecipe createRecipe(String id, Ingredient ingredient, FluidStack fromFluid,
			FluidStack toFluid) {
		return new ProcessingRecipeBuilder<>(MixingRecipe::new, Create.asResource(id)).require(ingredient)
				.require(FluidIngredient.fromFluidStack(fromFluid)).output(toFluid).requiresHeat(HeatCondition.HEATED)
				.build();
	}

	private static Map<Item, List<MixingRecipe>> sortRecipesByItem(List<MixingRecipe> all) {
		Map<Item, List<MixingRecipe>> byItem = new HashMap<>();
		Set<Item> processedItems = new HashSet<>();
		for (MixingRecipe recipe : all) {
			for (Ingredient ingredient : recipe.getIngredients()) {
				for (ItemStack itemStack : ingredient.getItems()) {
					Item item = itemStack.getItem();
					if (processedItems.add(item)) {
						byItem.computeIfAbsent(item, i -> new ArrayList<>()).add(recipe);
					}
				}
			}
			processedItems.clear();
		}
		return byItem;
	}

}
