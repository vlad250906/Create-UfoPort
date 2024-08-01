package com.simibubi.create.content.processing.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.SimpleDatagenIngredient;
import com.simibubi.create.foundation.data.recipe.Mods;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.simibubi.create.foundation.utility.Pair;
import com.tterrag.registrate.util.DataIngredient;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

public class ProcessingRecipeBuilder<T extends ProcessingRecipe<?>> {

	protected ProcessingRecipeFactory<T> factory;
	protected ProcessingRecipeParams params;
	protected List<ResourceCondition> recipeConditions;

	public ProcessingRecipeBuilder(ProcessingRecipeFactory<T> factory, ResourceLocation recipeId) {
		params = new ProcessingRecipeParams(recipeId);
		recipeConditions = new ArrayList<>();
		this.factory = factory;
	}

	public ProcessingRecipeBuilder<T> withItemIngredients(Ingredient... ingredients) {
		return withItemIngredients(NonNullList.of(Ingredient.EMPTY, ingredients));
	}

	public ProcessingRecipeBuilder<T> withItemIngredients(NonNullList<Ingredient> ingredients) {
		params.ingredients = ingredients;
		return this;
	}

	public ProcessingRecipeBuilder<T> withSingleItemOutput(ItemStack output) {
		return withItemOutputs(new ProcessingOutput(output, 1));
	}

	public ProcessingRecipeBuilder<T> withItemOutputs(ProcessingOutput... outputs) {
		return withItemOutputs(NonNullList.of(ProcessingOutput.EMPTY, outputs));
	}

	public ProcessingRecipeBuilder<T> withItemOutputs(NonNullList<ProcessingOutput> outputs) {
		params.results = outputs;
		return this;
	}

	public ProcessingRecipeBuilder<T> withFluidIngredients(FluidIngredient... ingredients) {
		return withFluidIngredients(NonNullList.of(FluidIngredient.EMPTY, ingredients));
	}

	public ProcessingRecipeBuilder<T> withFluidIngredients(NonNullList<FluidIngredient> ingredients) {
		params.fluidIngredients = ingredients;
		return this;
	}

	public ProcessingRecipeBuilder<T> withFluidOutputs(FluidStack... outputs) {
		return withFluidOutputs(NonNullList.of(FluidStack.EMPTY, outputs));
	}

	public ProcessingRecipeBuilder<T> withFluidOutputs(NonNullList<FluidStack> outputs) {
		params.fluidResults = outputs;
		return this;
	}

	public ProcessingRecipeBuilder<T> duration(int ticks) {
		params.processingDuration = ticks;
		return this;
	}

	public ProcessingRecipeBuilder<T> averageProcessingDuration() {
		return duration(100);
	}

	public ProcessingRecipeBuilder<T> requiresHeat(HeatCondition condition) {
		params.requiredHeat = condition;
		return this;
	}

	public T build() {
		validateFluidAmounts();
		return factory.create(params);
	}

	public void build(RecipeOutput consumer) {
		final ResourceCondition[] conds = new ResourceCondition[recipeConditions.size()];
		for(int i=0;i<conds.length;i++) {
			conds[i] = recipeConditions.get(i);
		}
		
		RecipeOutput consumer2 = new RecipeOutput() {

			@Override
			public void accept(ResourceLocation var1, Recipe<?> var2, AdvancementHolder var3) {
				if(conds.length != 0)
					FabricDataGenHelper.addConditions(var2, conds);
				consumer.accept(var1, var2, var3);
			}

			@Override
			public Builder advancement() {
				return consumer.advancement();
			}
			
		};
		
		ProcessingRecipe<?> recipe = build();
		ResourceLocation typeId = recipe.getTypeInfo().getId();
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(params.id.getNamespace(),
				typeId.getPath() + "/" + params.id.getPath());
		consumer2.accept(id, recipe, null);
	}

	public static final long[] SUS_AMOUNTS = { 10, 250, 500, 1000 };

	private void validateFluidAmounts() {
		for (FluidIngredient ingredient : params.fluidIngredients) {
			for (long amount : SUS_AMOUNTS) {
				if (ingredient.getRequiredAmount() == amount) {
					Create.LOGGER.warn("Suspicious fluid amount in recipe [{}]: {}", params.id, amount);
				}
			}
		}
	}

	// Datagen shortcuts

	public ProcessingRecipeBuilder<T> require(TagKey<Item> tag) {
		return require(Ingredient.of(tag));
	}

	public ProcessingRecipeBuilder<T> require(ItemLike item) {
		return require(Ingredient.of(item));
	}

	public ProcessingRecipeBuilder<T> require(Ingredient ingredient) {
		params.ingredients.add(ingredient);
		return this;
	}

	// fabric: custom ingredient support
	public ProcessingRecipeBuilder<T> require(CustomIngredient ingredient) {
		return require(ingredient.toVanilla());
	}

	public ProcessingRecipeBuilder<T> require(Mods mod, String id) {
		params.ingredients.add(new SimpleDatagenIngredient(mod, id));
		return this;
	}

	public ProcessingRecipeBuilder<T> require(ResourceLocation ingredient) {
		params.ingredients.add(DataIngredient.ingredient(null, ingredient));
		return this;
	}

	public ProcessingRecipeBuilder<T> require(Fluid fluid, long amount) {
		return require(FluidIngredient.fromFluid(fluid, amount));
	}

	public ProcessingRecipeBuilder<T> require(TagKey<Fluid> fluidTag, long amount) {
		return require(FluidIngredient.fromTag(fluidTag, amount));
	}

	public ProcessingRecipeBuilder<T> require(FluidIngredient ingredient) {
		params.fluidIngredients.add(ingredient);
		return this;
	}

	public ProcessingRecipeBuilder<T> output(ItemLike item) {
		return output(item, 1);
	}

	public ProcessingRecipeBuilder<T> output(float chance, ItemLike item) {
		return output(chance, item, 1);
	}

	public ProcessingRecipeBuilder<T> output(ItemLike item, int amount) {
		return output(1, item, amount);
	}

	public ProcessingRecipeBuilder<T> output(float chance, ItemLike item, int amount) {
		return output(chance, new ItemStack(item, amount));
	}

	public ProcessingRecipeBuilder<T> output(ItemStack output) {
		return output(1, output);
	}

	public ProcessingRecipeBuilder<T> output(float chance, ItemStack output) {
		return output(new ProcessingOutput(output, chance));
	}

	public ProcessingRecipeBuilder<T> output(float chance, Mods mod, String id, int amount) {
		return output(new ProcessingOutput(Pair.of(mod.asResource(id), amount), chance));
	}

	public ProcessingRecipeBuilder<T> output(float chance, ResourceLocation registryName, int amount) {
		return output(new ProcessingOutput(Pair.of(registryName, amount), chance));
	}

	public ProcessingRecipeBuilder<T> output(ProcessingOutput output) {
		params.results.add(output);
		return this;
	}

	public ProcessingRecipeBuilder<T> output(Fluid fluid, long amount) {
		fluid = FluidHelper.convertToStill(fluid);
		return output(new FluidStack(fluid, amount));
	}

	public ProcessingRecipeBuilder<T> output(FluidStack fluidStack) {
		params.fluidResults.add(fluidStack);
		return this;
	}

	public ProcessingRecipeBuilder<T> toolNotConsumed() {
		params.keepHeldItem = true;
		return this;
	}

	//

	public ProcessingRecipeBuilder<T> whenModLoaded(String modid) {
		
		return withCondition(ResourceConditions.allModsLoaded(modid));
	}

	public ProcessingRecipeBuilder<T> whenModMissing(String modid) {
		return withCondition(ResourceConditions.not(ResourceConditions.allModsLoaded(modid)));
	}

	public ProcessingRecipeBuilder<T> withCondition(ResourceCondition condition) {
		recipeConditions.add(condition);
		return this;
	}

	@FunctionalInterface
	public interface ProcessingRecipeFactory<T extends ProcessingRecipe<?>> {
		T create(ProcessingRecipeParams params);

	}

	public static class ProcessingRecipeParams {

		protected ResourceLocation id;
		protected NonNullList<Ingredient> ingredients;
		protected NonNullList<ProcessingOutput> results;
		protected NonNullList<FluidIngredient> fluidIngredients;
		protected NonNullList<FluidStack> fluidResults;
		protected int processingDuration;
		protected HeatCondition requiredHeat;

		public boolean keepHeldItem;

		protected ProcessingRecipeParams(ResourceLocation id) {
			this.id = id;
			ingredients = NonNullList.create();
			results = NonNullList.create();
			fluidIngredients = NonNullList.create();
			fluidResults = NonNullList.create();
			processingDuration = 0;
			requiredHeat = HeatCondition.NONE;
			keepHeldItem = false;
		}
	}

//	public static class DataGenResult<S extends ProcessingRecipe<?>> implements RecipeOutput {
//
//		private List<ResourceCondition> recipeConditions;
//		private RecipeOutput wrapper;
//		private ResourceLocation id;
//		private S recipe;
//		
//		
//		@SuppressWarnings("unchecked")
//		public DataGenResult(S recipe, List<ResourceCondition> recipeConditions) {
//			this.recipe = recipe;
//			this.recipeConditions = recipeConditions;
//			IRecipeTypeInfo recipeType = this.recipe.getTypeInfo();
//			ResourceLocation typeId = recipeType.getId();
//
//			if (!(recipeType.getSerializer() instanceof ProcessingRecipeSerializer))
//				throw new IllegalStateException("Cannot datagen ProcessingRecipe of type: " + typeId);
//
//			this.id = ResourceLocation.fromNamespaceAndPath(recipe.getId().getNamespace(),
//					typeId.getPath() + "/" + recipe.getId().getPath());
//			this.serializer = (ProcessingRecipeSerializer<S>) recipe.getSerializer();
//		}
//
//		@Override
//		public void serializeRecipeData(JsonObject json) {
//			serializer.write(json, recipe);
//			if (recipeConditions.isEmpty())
//				return;
//
//			JsonArray conds = new JsonArray();
//			recipeConditions.forEach(c -> conds.add(c.toJson())); // FabricDataGenHelper.addConditions(json, recipeConditions.toArray());?
//			json.add(ResourceConditions.CONDITIONS_KEY, conds);
//		}
//
//		@Override
//		public void accept(ResourceLocation location, Recipe<?> var2, AdvancementHolder var3) {
//			
//		}
//
//		@Override
//		public Builder advancement() {
//			
//		}
//
//	}

}
