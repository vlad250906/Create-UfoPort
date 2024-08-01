package com.simibubi.create.content.processing.sequenced;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeFactory;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

public class SequencedAssemblyRecipeBuilder {

	private SequencedAssemblyRecipe recipe;
	protected List<ResourceCondition> recipeConditions;

	public SequencedAssemblyRecipeBuilder(ResourceLocation id) {
		recipeConditions = new ArrayList<>();
		this.recipe = new SequencedAssemblyRecipe(id,
			AllRecipeTypes.SEQUENCED_ASSEMBLY.getSerializer());
	}

	public <T extends ProcessingRecipe<?>> SequencedAssemblyRecipeBuilder addStep(ProcessingRecipeFactory<T> factory,
		UnaryOperator<ProcessingRecipeBuilder<T>> builder) {
		ProcessingRecipeBuilder<T> recipeBuilder =
			new ProcessingRecipeBuilder<>(factory, ResourceLocation.parse("dummy"));
		Item placeHolder = recipe.getTransitionalItem()
			.getItem();
		recipe.getSequence()
			.add(new SequencedRecipe<>(builder.apply(recipeBuilder.require(placeHolder)
				.output(placeHolder))
				.build()));
		return this;
	}

	public SequencedAssemblyRecipeBuilder require(ItemLike ingredient) {
		return require(Ingredient.of(ingredient));
	}

	public SequencedAssemblyRecipeBuilder require(TagKey<Item> tag) {
		return require(Ingredient.of(tag));
	}

	public SequencedAssemblyRecipeBuilder require(Ingredient ingredient) {
		recipe.ingredient = ingredient;
		return this;
	}

	public SequencedAssemblyRecipeBuilder transitionTo(ItemLike item) {
		recipe.transitionalItem = new ProcessingOutput(new ItemStack(item), 1);
		return this;
	}

	public SequencedAssemblyRecipeBuilder loops(int loops) {
		recipe.loops = loops;
		return this;
	}

	public SequencedAssemblyRecipeBuilder addOutput(ItemLike item, float weight) {
		return addOutput(new ItemStack(item), weight);
	}

	public SequencedAssemblyRecipeBuilder addOutput(ItemStack item, float weight) {
		recipe.resultPool.add(new ProcessingOutput(item, weight));
		return this;
	}

	public SequencedAssemblyRecipe build() {
		return recipe;
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
		
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(recipe.id.getNamespace(),
				AllRecipeTypes.SEQUENCED_ASSEMBLY.getId().getPath() + "/" + recipe.id.getPath());
		consumer2.accept(id, build(), null);
	}

	public static class DataGenResult implements RecipeOutput {

		private SequencedAssemblyRecipe recipe;
		private List<ResourceConditions> recipeConditions;
		private ResourceLocation id = ResourceLocation.parse("fukfukDataGenResult");
		private SequencedAssemblyRecipeSerializer serializer;

		public DataGenResult(SequencedAssemblyRecipe recipe, List<ResourceConditions> recipeConditions) {
			this.recipeConditions = recipeConditions;
			this.recipe = recipe;
			//this.id = ResourceLocation.fromNamespaceAndPath(recipe.getId().getNamespace(),
					//AllRecipeTypes.SEQUENCED_ASSEMBLY.getId().getPath() + "/" + recipe.getId().getPath());
			this.serializer = (SequencedAssemblyRecipeSerializer) recipe.getSerializer();
		}

		/*@Override
		public void serializeRecipeData(JsonObject json) {
			serializer.write(json, recipe);
			if (recipeConditions.isEmpty())
				return;

			JsonArray conds = new JsonArray();
			recipeConditions.forEach(c -> conds.add(c.toJson()));
			json.add(ResourceConditions.CONDITIONS_KEY, conds);
		}

		@Override
		public ResourceLocation getId() {
			return id;
		}

		@Override
		public RecipeSerializer<?> getType() {
			return serializer;
		}

		@Override
		public JsonObject serializeAdvancement() {
			return null;
		}

		@Override
		public ResourceLocation getAdvancementId() {
			return null;
		}*/

		@Override
		public void accept(ResourceLocation var1, Recipe<?> var2, AdvancementHolder var3) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Builder advancement() {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
