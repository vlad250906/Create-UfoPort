package io.github.tropheusj.milk.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.tropheusj.milk.Milk;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;

@Mixin(BrewingStandMenu.PotionSlot.class)
public abstract class BrewingStandScreenHandlerPotionSlotMixin {
	@Inject(method = "mayPlaceItem", at = @At("HEAD"), cancellable = true)
	private static void milk$mayPlaceItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (stack.is(Milk.SPLASH_MILK_BOTTLE) || stack.is(Milk.LINGERING_MILK_BOTTLE) || stack.is(Milk.MILK_BOTTLE)) {
			cir.setReturnValue(true);
		}
	}
}
