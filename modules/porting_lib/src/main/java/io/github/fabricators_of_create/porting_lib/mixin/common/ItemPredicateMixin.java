package io.github.fabricators_of_create.porting_lib.mixin.common;

import com.google.gson.JsonElement;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import io.github.fabricators_of_create.porting_lib.util.ItemPredicateRegistry;
import net.minecraft.advancements.critereon.ItemPredicate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ItemPredicate.class)
public abstract class ItemPredicateMixin {
	
}
