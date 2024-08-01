package com.simibubi.create.compat.jei.category.sequencedAssembly;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedDeployer;
import com.simibubi.create.compat.jei.category.animations.AnimatedPress;
import com.simibubi.create.compat.jei.category.animations.AnimatedSaw;
import com.simibubi.create.compat.jei.category.animations.AnimatedSpout;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.utility.Lang;

import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;

public abstract class JeiSequencedAssemblySubCategory {

	private final int width;

	public JeiSequencedAssemblySubCategory(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public void setRecipe(IRecipeLayoutBuilder builder, SequencedRecipe<?> recipe, IFocusGroup focuses, int x) {}

	public abstract void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index);

	public static class AssemblyPressing extends JeiSequencedAssemblySubCategory {

		AnimatedPress press;

		public AssemblyPressing() {
			super(25);
			press = new AnimatedPress(false);
		}

		@Override
		public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {
			PoseStack ms = graphics.pose();
			press.offset = index;
			ms.pushPose();
			ms.translate(-5, 50, 0);
			ms.scale(.6f, .6f, .6f);
			press.draw(graphics, getWidth() / 2, 0);
			ms.popPose();
		}

	}

	public static class AssemblySpouting extends JeiSequencedAssemblySubCategory {

		AnimatedSpout spout;

		public AssemblySpouting() {
			super(25);
			spout = new AnimatedSpout();
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, SequencedRecipe<?> recipe, IFocusGroup focuses, int x) {
			FluidIngredient fluidIngredient = recipe.getRecipe()
					.getFluidIngredients()
					.get(0);

			builder
					.addSlot(RecipeIngredientRole.INPUT, x + 4, 15)
					.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
					.addIngredients(FabricTypes.FLUID_STACK, CreateRecipeCategory.toJei(CreateRecipeCategory.withImprovedVisibility(fluidIngredient.getMatchingFluidStacks())))
					.addTooltipCallback(CreateRecipeCategory.addFluidTooltip(fluidIngredient.getRequiredAmount()));
		}

		@Override
		public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {
			PoseStack ms = graphics.pose();
			spout.offset = index;
			ms.pushPose();
			ms.translate(-7, 50, 0);
			ms.scale(.75f, .75f, .75f);
			spout.withFluids(recipe.getRecipe()
				.getFluidIngredients()
				.get(0)
				.getMatchingFluidStacks())
				.draw(graphics, getWidth() / 2, 0);
			ms.popPose();
		}

	}

	public static class AssemblyDeploying extends JeiSequencedAssemblySubCategory {

		AnimatedDeployer deployer;

		public AssemblyDeploying() {
			super(25);
			deployer = new AnimatedDeployer();
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, SequencedRecipe<?> recipe, IFocusGroup focuses, int x) {
			IRecipeSlotBuilder slot = builder
					.addSlot(RecipeIngredientRole.INPUT, x + 4, 15)
					.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
					.addIngredients(recipe.getRecipe().getIngredients().get(1));

			if (recipe.getAsAssemblyRecipe() instanceof DeployerApplicationRecipe deployerRecipe && deployerRecipe.shouldKeepHeldItem()) {
				slot.addTooltipCallback(
						(recipeSlotView, tooltip) -> tooltip.add(1, Lang.translateDirect("recipe.deploying.not_consumed").withStyle(ChatFormatting.GOLD))
				);
			}
		}

		@Override
		public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {
			PoseStack ms = graphics.pose();
			deployer.offset = index;
			ms.pushPose();
			ms.translate(-7, 50, 0);
			ms.scale(.75f, .75f, .75f);
			deployer.draw(graphics, getWidth() / 2, 0);
			ms.popPose();
		}

	}

	public static class AssemblyCutting extends JeiSequencedAssemblySubCategory {

		AnimatedSaw saw;

		public AssemblyCutting() {
			super(25);
			saw = new AnimatedSaw();
		}

		@Override
		public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {
			PoseStack ms = graphics.pose();
			ms.pushPose();
			ms.translate(0, 51.5f, 0);
			ms.scale(.6f, .6f, .6f);
			saw.draw(graphics, getWidth() / 2, 30);
			ms.popPose();
		}

	}

}