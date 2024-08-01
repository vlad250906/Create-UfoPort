package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Screen.class)
public class ScreenMixin {
	
	@WrapWithCondition(method = "render", at = @At(target = "Lnet/minecraft/client/gui/screens/Screen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", value = "INVOKE"))
	private boolean create$disableBackgroundCall(Screen scr, GuiGraphics gr, int x, int y, float f) {
		//LogUtils.getLogger().info(this+"");
		if((Screen)(Object)this instanceof AbstractSimiScreen) {
			return false;
		}
		return true;
	}
}
