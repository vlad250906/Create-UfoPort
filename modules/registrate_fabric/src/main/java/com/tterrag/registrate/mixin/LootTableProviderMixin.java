package com.tterrag.registrate.mixin;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;
import com.tterrag.registrate.fabric.CustomValidationLootProvider;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.ValidationContext;

@Mixin(LootTableProvider.class)
public class LootTableProviderMixin {
//    @ModifyExpressionValue(
//            method = "Lnet/minecraft/data/loot/LootTableProvider;run(Lnet/minecraft/data/CachedOutput;Lnet/minecraft/core/HolderLookup$Provider;)Ljava/util/concurrent/CompletableFuture;",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Ljava/util/Set;iterator()Ljava/util/Iterator;" // Sets.difference
//            )
//    )
//    private Iterator runCustomValidation(Iterator original,
//    		  @Local Map writableRegistry,
//	          @Local ValidationContext context) {
////    	
////    	LogUtils.getLogger().error("WritableRegistry is: "+writableRegistry);
////    	if(1 == 1)
////    		throw new RuntimeException("Fuck fuck fuck fuck in LootTableProvider!!!!!");
////        if (this instanceof CustomValidationLootProvider custom) {
////            custom.validate(tables, context);
////            return Collections.emptyIterator();
////        }
////        return original;
//    }

//    @WrapWithCondition(
//            method = "Lnet/minecraft/data/loot/LootTableProvider;run(Lnet/minecraft/data/CachedOutput;Lnet/minecraft/core/HolderLookup$Provider;)Ljava/util/concurrent/CompletableFuture;",
//                at = @At(
//                        value = "INVOKE",
//                        target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"
//                )
//    )
//    private boolean preventOtherValidation(Stream tables, Consumer consumer) {
//        return !(this instanceof CustomValidationLootProvider);
//    }
}
