package com.simibubi.create.foundation.data.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.foundation.utility.RegisteredObjects;

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
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

public class MechanicalCraftingRecipeBuilder {

	private final Item result;
	private final int count;
	private final List<String> pattern = Lists.newArrayList();
	private final Map<Character, Ingredient> key = Maps.newLinkedHashMap();
	private boolean acceptMirrored;
	private List<ResourceCondition> recipeConditions;

	public MechanicalCraftingRecipeBuilder(ItemLike p_i48261_1_, int p_i48261_2_) {
		result = p_i48261_1_.asItem();
		count = p_i48261_2_;
		acceptMirrored = true;
		recipeConditions = new ArrayList<>();
	}

	/**
	 * Creates a new builder for a shaped recipe.
	 */
	public static MechanicalCraftingRecipeBuilder shapedRecipe(ItemLike p_200470_0_) {
		return shapedRecipe(p_200470_0_, 1);
	}

	/**
	 * Creates a new builder for a shaped recipe.
	 */
	public static MechanicalCraftingRecipeBuilder shapedRecipe(ItemLike p_200468_0_, int p_200468_1_) {
		return new MechanicalCraftingRecipeBuilder(p_200468_0_, p_200468_1_);
	}

	/**
	 * Adds a key to the recipe pattern.
	 */
	public MechanicalCraftingRecipeBuilder key(Character p_200469_1_, TagKey<Item> p_200469_2_) {
		return this.key(p_200469_1_, Ingredient.of(p_200469_2_));
	}

	/**
	 * Adds a key to the recipe pattern.
	 */
	public MechanicalCraftingRecipeBuilder key(Character p_200462_1_, ItemLike p_200462_2_) {
		return this.key(p_200462_1_, Ingredient.of(p_200462_2_));
	}

	/**
	 * Adds a key to the recipe pattern.
	 */
	public MechanicalCraftingRecipeBuilder key(Character p_200471_1_, Ingredient p_200471_2_) {
		if (this.key.containsKey(p_200471_1_)) {
			throw new IllegalArgumentException("Symbol '" + p_200471_1_ + "' is already defined!");
		} else if (p_200471_1_ == ' ') {
			throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
		} else {
			this.key.put(p_200471_1_, p_200471_2_);
			return this;
		}
	}

	/**
	 * Adds a new entry to the patterns for this recipe.
	 */
	public MechanicalCraftingRecipeBuilder patternLine(String p_200472_1_) {
		if (!this.pattern.isEmpty() && p_200472_1_.length() != this.pattern.get(0)
			.length()) {
			throw new IllegalArgumentException("Pattern must be the same width on every line!");
		} else {
			this.pattern.add(p_200472_1_);
			return this;
		}
	}

	/**
	 * Prevents the crafters from matching a vertically flipped version of the recipe
	 */
	public MechanicalCraftingRecipeBuilder disallowMirrored() {
		acceptMirrored = false;
		return this;
	}

	/**
	 * Builds this recipe into a {@link FinishedRecipe}.
	 */
	public void build(RecipeOutput p_200464_1_) {
		this.build(p_200464_1_, RegisteredObjects.getKeyOrThrow(this.result));
	}

	/**
	 * Builds this recipe into a {@link FinishedRecipe}. Use
	 * {@link #build(Consumer)} if save is the same as the ID for the result.
	 */
	public void build(RecipeOutput p_200466_1_, String p_200466_2_) {
		ResourceLocation resourcelocation = RegisteredObjects.getKeyOrThrow(this.result);
		if ((ResourceLocation.parse(p_200466_2_)).equals(resourcelocation)) {
			throw new IllegalStateException("Shaped Recipe " + p_200466_2_ + " should remove its 'save' argument");
		} else {
			this.build(p_200466_1_, ResourceLocation.parse(p_200466_2_));
		}
	}

	/**
	 * Builds this recipe into a {@link FinishedRecipe}.
	 */
	public void build(RecipeOutput consumer, ResourceLocation id) {
		validate(id);
		
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
		
		ShapedRecipe shaped = new ShapedRecipe("undefined", CraftingBookCategory.MISC, 
				ShapedRecipePattern.of(key, pattern), new ItemStack(result, count), acceptMirrored);
		MechanicalCraftingRecipe recipe = MechanicalCraftingRecipe.fromShaped(shaped, acceptMirrored);
		
		consumer2.accept(id, recipe, null);
	}

	/**
	 * Makes sure that this recipe is valid.
	 */
	private void validate(ResourceLocation p_200463_1_) {
		if (pattern.isEmpty()) {
			throw new IllegalStateException("No pattern is defined for shaped recipe " + p_200463_1_ + "!");
		} else {
			Set<Character> set = Sets.newHashSet(key.keySet());
			set.remove(' ');

			for (String s : pattern) {
				for (int i = 0; i < s.length(); ++i) {
					char c0 = s.charAt(i);
					if (!key.containsKey(c0) && c0 != ' ')
						throw new IllegalStateException(
							"Pattern in recipe " + p_200463_1_ + " uses undefined symbol '" + c0 + "'");
					set.remove(c0);
				}
			}

			if (!set.isEmpty())
				throw new IllegalStateException(
					"Ingredients are defined but not used in pattern for recipe " + p_200463_1_);
		}
	}

	public MechanicalCraftingRecipeBuilder whenModLoaded(String modid) {
		return withCondition(ResourceConditions.allModsLoaded(modid));
	}

	public MechanicalCraftingRecipeBuilder whenModMissing(String modid) {
		return withCondition(ResourceConditions.not(ResourceConditions.allModsLoaded(modid)));
	}

	public MechanicalCraftingRecipeBuilder withCondition(ResourceCondition condition) {
		recipeConditions.add(condition);
		return this;
	}

}
