package io.github.fabricators_of_create.porting_lib.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.fabricators_of_create.porting_lib.enchant.CustomEnchantingBehaviorItem;
import io.github.fabricators_of_create.porting_lib.enchant.CustomEnchantingTableBehaviorEnchantment;
import io.github.fabricators_of_create.porting_lib.item.CustomEnchantmentLevelItem;
import io.github.fabricators_of_create.porting_lib.item.CustomEnchantmentsItem;
import net.minecraft.core.Holder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

//	@ModifyExpressionValue(
//			method = "getAvailableEnchantmentResults",
//			at = @At(
//					value = "INVOKE",
//					target = "Lnet/minecraft/core/Holder;value()Ljava/lang/Object;"
//			)
//	)

//	/**
//	 * Same behavior as {@link EnchantmentMixin#port_lib$canEnchant(ItemStack, CallbackInfoReturnable)}
//	 */
//	@SuppressWarnings("JavadocReference")
//	@WrapOperation(
//			method = "getAvailableEnchantmentResults",
//			at = @At(
//					value = "INVOKE",
//					target = "Lnet/minecraft/world/item/enchantment/Enchantment;isPrimaryItem(Lnet/minecraft/world/item/ItemStack;)Z"
//			)
//	)
//	private static boolean port_lib$customEnchantability(Enchantment category, ItemStack item, Operation<Boolean> original,
//														FeatureFlagSet enabledFeatures, int level, ItemStack stack, boolean allowTreasure) {
//		Enchantment enchantment = port_lib$currentEnchantment;
//		
//		/*
//		  if (enchantment instanceof CustomEnchantingTableBehaviorEnchantment custom) {
//			// custom enchantment? let the custom logic take over
//			return custom.canApplyAtEnchantingTable(stack);
//		} else 
//		
//		 */
//		
//		if (enchantment != null && stack.getItem() instanceof CustomEnchantingBehaviorItem custom) {
//			// enchantment not custom, but item is - let item decide
//			return custom.canApplyAtEnchantingTable(stack, enchantment);
//		}
//		// neither - vanilla logic
//		return original.call(category, item);
//	}

	@ModifyReturnValue(method = "getItemEnchantmentLevel", at = @At("RETURN"))
	private static int modifyEnchantmentLevel(int original, Holder<Enchantment> enchantment, ItemStack stack) {
		if (stack.getItem() instanceof CustomEnchantmentLevelItem custom)
			return custom.modifyEnchantmentLevel(stack, enchantment.value(), original);
		return original;
	}

	@ModifyReturnValue(method = "getEnchantmentsForCrafting", at = @At("RETURN"))
	private static ItemEnchantments customEnchantments(ItemEnchantments enchantments, ItemStack stack) {
//		if (!(enchantments instanceof HashMap)) // mutability is expected, fix it if something else changed it
//			enchantments = new LinkedHashMap<>(enchantments);

		if (stack.getItem() instanceof CustomEnchantmentsItem custom) {
			ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(enchantments);
			custom.modifyEnchantments(mut, stack);
			enchantments = mut.toImmutable();
		}
		return enchantments;
	}

	@Inject(method = "runIterationOnItem", at = @At("HEAD"), cancellable = true)
	private static void useCustomEnchantmentList(ItemStack stack, EnchantmentHelper.EnchantmentVisitor visitor, CallbackInfo ci) {
		if (stack.getItem() instanceof CustomEnchantmentsItem) {
			EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet().forEach((holder) -> {
				int level = EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
				visitor.accept(holder, level);
			});
			ci.cancel();
		}
	}
}
