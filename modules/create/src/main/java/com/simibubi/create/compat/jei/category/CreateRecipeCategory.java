package com.simibubi.create.compat.jei.category;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.AllFluids;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.util.FluidTextUtil;
import io.github.fabricators_of_create.porting_lib.util.FluidUnit;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotTooltipCallback;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CreateRecipeCategory<T extends Recipe<?>> implements IRecipeCategory<T> {
	private static final IDrawable BASIC_SLOT = asDrawable(AllGuiTextures.JEI_SLOT);
	private static final IDrawable CHANCE_SLOT = asDrawable(AllGuiTextures.JEI_CHANCE_SLOT);

	protected final RecipeType<T> type;
	protected final Component title;
	protected final IDrawable background;
	protected final IDrawable icon;

	private final Supplier<List<T>> recipes;
	private final List<Supplier<? extends ItemStack>> catalysts;

	public CreateRecipeCategory(Info<T> info) {
		this.type = info.recipeType();
		this.title = info.title();
		this.background = info.background();
		this.icon = info.icon();
		this.recipes = info.recipes();
		this.catalysts = info.catalysts();
	}

	@NotNull
	@Override
	public RecipeType<T> getRecipeType() {
		return type;
	}

	@Override
	public Component getTitle() {
		return title;
	}

	@Override
	public IDrawable getBackground() {
		return background;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(type, recipes.get().stream().map(inp -> inp).toList());
	}

	public void registerCatalysts(IRecipeCatalystRegistration registration) {
		catalysts.forEach(s -> registration.addRecipeCatalyst(s.get(), type));
	}

	public static IDrawable getRenderedSlot() {
		return BASIC_SLOT;
	}

	public static IDrawable getRenderedSlot(ProcessingOutput output) {
		return getRenderedSlot(output.getChance());
	}

	public static IDrawable getRenderedSlot(float chance) {
		if (chance == 1)
			return BASIC_SLOT;

		return CHANCE_SLOT;
	}

	public static ItemStack getResultItem(Recipe<?> recipe) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return ItemStack.EMPTY;
		return recipe.getResultItem(level.registryAccess());
	}

	public static IRecipeSlotTooltipCallback addStochasticTooltip(ProcessingOutput output) {
		return (view, tooltip) -> {
			float chance = output.getChance();
			if (chance != 1)
				tooltip.add(1,
						Lang.translateDirect("recipe.processing.chance", chance < 0.01 ? "<1" : (int) (chance * 100))
								.withStyle(ChatFormatting.GOLD));
		};
	}

	public static List<FluidStack> withImprovedVisibility(List<FluidStack> stacks) {
		return stacks.stream().map(CreateRecipeCategory::withImprovedVisibility).collect(Collectors.toList());
	}

	public static FluidStack withImprovedVisibility(FluidStack stack) {
		FluidStack display = stack.copy();
		long displayedAmount = (long) (stack.getAmount() * .75f) + 250;
		display.setAmount(displayedAmount);
		return display;
	}

	public static FluidStack fromJei(IJeiFluidIngredient jei) {
		return new FluidStack(jei.getFluidVariant(), jei.getAmount());
	}

	public static IJeiFluidIngredient toJei(FluidStack stack) {
		return new IJeiFluidIngredient() {
			

			@Override
			public long getAmount() {
				return stack.getAmount();
			}

			@Override
			public FluidVariant getFluidVariant() {
				return stack.getType();
			}
		};
	}

	public static List<FluidStack> fromJei(List<IJeiFluidIngredient> stacks) {
		return stacks.stream().map(CreateRecipeCategory::fromJei).toList();
	}

	public static List<IJeiFluidIngredient> toJei(List<FluidStack> stacks) {
		return stacks.stream().map(CreateRecipeCategory::toJei).toList();
	}

	public static IRecipeSlotTooltipCallback addFluidTooltip() {
		return addFluidTooltip(-1);
	}

	public static IRecipeSlotTooltipCallback addFluidTooltip(long mbAmount) {
		return (view, tooltip) -> {
			Optional<IJeiFluidIngredient> displayed = view.getDisplayedIngredient(FabricTypes.FLUID_STACK);
			if (displayed.isEmpty())
				return;

			FluidStack fluidStack = fromJei(displayed.get());

			// fabric: don't need potion tooltip stuff, handled by attribute handler

			long amountToUse = mbAmount == -1 ? fluidStack.getAmount() : mbAmount;
			FluidUnit unit = AllConfigs.client().fluidUnitType.get();
			String amount = FluidTextUtil.getUnicodeMillibuckets(amountToUse, unit,
					AllConfigs.client().simplifyFluidUnit.get());
			Component text = Component.literal(String.valueOf(amount))
					.append(Lang.translateDirect(unit.getTranslationKey())).withStyle(ChatFormatting.GOLD);
			if (tooltip.isEmpty())
				tooltip.add(0, text);
			else {
				// fabric: sibling strategy doesn't work some reason
				Component name = tooltip.get(0);
				Component nameWithAmount = name.copy().append(" ").append(text);
				tooltip.set(0, nameWithAmount);
			}
		};
	}

	protected static IDrawable asDrawable(AllGuiTextures texture) {
		return new IDrawable() {
			@Override
			public int getWidth() {
				return texture.width;
			}

			@Override
			public int getHeight() {
				return texture.height;
			}

			@Override
			public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
				texture.render(graphics, xOffset, yOffset);
			}
		};
	}

	public record Info<T extends Recipe<?>>(RecipeType<T> recipeType, Component title, IDrawable background,
			IDrawable icon, Supplier<List<T>> recipes, List<Supplier<? extends ItemStack>> catalysts) {
	}

	public interface Factory<T extends Recipe<?>> {
		CreateRecipeCategory<T> create(Info<T> info);
	}
}
