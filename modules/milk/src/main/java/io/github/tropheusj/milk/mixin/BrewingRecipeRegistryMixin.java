package io.github.tropheusj.milk.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.tropheusj.milk.Milk;
import net.minecraft.core.Holder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;

@Mixin(PotionBrewing.Builder.class)
public abstract class BrewingRecipeRegistryMixin {
	@Shadow
	@Final
	private List<Ingredient> containers;

	@Shadow
	@Final
	private List<PotionBrewing.Mix<Item>> containerMixes;

	@Inject(at = @At("HEAD"), method = "addContainer", cancellable = true)
	private void milk$addContainer(Item item, CallbackInfo ci) {
		if (Milk.isMilkBottle(item)) {
			containers.add(Ingredient.of(item));
			ci.cancel();
		}
	}
	
	@Inject(at = @At("TAIL"), method = "<init>")
	private void milk$initBuilder(FeatureFlagSet enabledFeatures, CallbackInfo inf) {
		Milk.enableAllMilkBottles((BrewingRecipeRegistryAccessor)(Object)this);
	}

	@Inject(at = @At("HEAD"), method = "addContainerRecipe", cancellable = true)
	private void milk$addContainerRecipe(Item input, Item ingredient, Item output, CallbackInfo ci) {
		if (Milk.isMilkBottle(input) && Milk.isMilkBottle(output)) {
			containerMixes.add(new PotionBrewing.Mix<Item>(input.builtInRegistryHolder(), Ingredient.of(ingredient), output.builtInRegistryHolder()));
			ci.cancel();
		}
	}
}
