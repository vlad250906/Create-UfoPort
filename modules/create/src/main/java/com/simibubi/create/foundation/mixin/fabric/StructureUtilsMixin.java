package com.simibubi.create.foundation.mixin.fabric;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

@Mixin(value = StructureUtils.class, priority = 900) // apply before FAPI, run earlier
public class StructureUtilsMixin {
	/**
	 * this is what vanilla and forge do, but FAPI forces a different system
	 * 
	 * @see StructureTestUtilMixin
	 */
	// @Inject(method = "getStructureTemplate", at = @At("HEAD"), cancellable =
	// true)
	// private static void useStructureManager(String name, ServerLevel level,
	// CallbackInfoReturnable<StructureTemplate> cir) {
	// ResourceLocation id = ResourceLocation.fromNamespaceAndPath(name);
	// level.getStructureManager().get(id).ifPresent(cir::setReturnValue);
	// }
}
