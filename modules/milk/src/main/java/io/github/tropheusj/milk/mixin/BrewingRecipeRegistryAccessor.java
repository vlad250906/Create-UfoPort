package io.github.tropheusj.milk.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionBrewing;

@Mixin(PotionBrewing.Builder.class)
public interface BrewingRecipeRegistryAccessor {
	@Invoker("addContainer")
	void milk$addContainer(Item item);

	@Invoker("addContainerRecipe")
	void milk$addContainerRecipe(Item input, Item ingredient, Item output);
}
