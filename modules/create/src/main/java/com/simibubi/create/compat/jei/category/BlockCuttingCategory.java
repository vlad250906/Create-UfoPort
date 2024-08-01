package com.simibubi.create.compat.jei.category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.compat.jei.category.BlockCuttingCategory.CondensedBlockCuttingRecipe;
import com.simibubi.create.compat.jei.category.animations.AnimatedSaw;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.item.ItemHelper;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

@ParametersAreNonnullByDefault
public class BlockCuttingCategory extends CreateRecipeCategory<CondensedBlockCuttingRecipe> {

	private final AnimatedSaw saw = new AnimatedSaw();

	public BlockCuttingCategory(Info<CondensedBlockCuttingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, CondensedBlockCuttingRecipe recipe, IFocusGroup focuses) {
		List<List<ItemStack>> results = recipe.getCondensedOutputs();

		builder
				.addSlot(RecipeIngredientRole.INPUT, 5, 5)
				.setBackground(getRenderedSlot(), -1 , -1)
				.addItemStacks(Arrays.asList(recipe.getIngredients().get(0).getItems()));

		int i = 0;
		for (List<ItemStack> itemStacks : results) {
			int xPos = 78 + (i % 5) * 19;
			int yPos = 48 + (i / 5) * -19;

			builder
					.addSlot(RecipeIngredientRole.OUTPUT, xPos, yPos)
					.setBackground(getRenderedSlot(), -1 , -1)
					.addItemStacks(itemStacks);
			i++;
		}
	}

	@Override
	public void draw(CondensedBlockCuttingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 31, 6);
		AllGuiTextures.JEI_SHADOW.render(graphics, 33 - 17, 37 + 13);
		saw.draw(graphics, 33, 37);
	}

	public static class CondensedBlockCuttingRecipe extends StonecutterRecipe {

		List<ItemStack> outputs = new ArrayList<>();

		public CondensedBlockCuttingRecipe(Ingredient ingredient) {
			super("", ingredient, ItemStack.EMPTY);
		}

		public void addOutput(ItemStack stack) {
			outputs.add(stack);
		}

		public List<ItemStack> getOutputs() {
			return outputs;
		}

		public List<List<ItemStack>> getCondensedOutputs() {
			List<List<ItemStack>> result = new ArrayList<>();
			int index = 0;
			boolean firstPass = true;
			for (ItemStack itemStack : outputs) {
				if (firstPass)
					result.add(new ArrayList<>());
				result.get(index).add(itemStack);
				index++;
				if (index >= 15) {
					index = 0;
					firstPass = false;
				}
			}
			return result;
		}

		@Override
		public boolean isSpecial() {
			return true;
		}

	}
	
	public static List<CondensedBlockCuttingRecipe> condenseRecipes(List<Recipe<?>> stoneCuttingRecipes) {
		List<CondensedBlockCuttingRecipe> condensed = new ArrayList<>();
		Recipes: for (Recipe<?> recipe : stoneCuttingRecipes) {
			Ingredient i1 = recipe.getIngredients().get(0);
			for (CondensedBlockCuttingRecipe condensedRecipe : condensed) {
				if (ItemHelper.matchIngredients(i1, condensedRecipe.getIngredients().get(0))) {
					condensedRecipe.addOutput(getResultItem(recipe));
					continue Recipes;
				}
			}
			CondensedBlockCuttingRecipe cr = new CondensedBlockCuttingRecipe(i1);
			cr.addOutput(getResultItem(recipe));
			condensed.add(cr);
		}
		return condensed;
	}

}
