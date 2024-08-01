package io.github.tropheusj.milk.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.mojang.blaze3d.systems.RenderSystem;

import io.github.tropheusj.milk.Milk;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;

@Environment(EnvType.CLIENT)
@Mixin(FogRenderer.class)
public abstract class BackgroundRendererMixin {
	@Shadow
	private static float fogRed;

	@Shadow
	private static float fogGreen;

	@Shadow
	private static float fogBlue;

	@ModifyArgs(method = "setupColor", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clearColor(FFFF)V", remap = false))
	private static void milk$modifyFogColors(Args args, Camera camera, float partialTicks, ClientLevel level, int renderDistanceChunks, float bossColorModifier) {
		FluidState state = level.getFluidState(camera.getBlockPosition());
		if (Milk.isMilk(state)) {
			fogRed = 1;
			fogGreen = 1;
			fogBlue = 1;
		}
	}

	@Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
	private static void milk$setupFog(Camera camera, FogMode fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
		FluidState state = Minecraft.getInstance().level.getFluidState(camera.getBlockPosition());
		if (Milk.isMilk(state)) {
			RenderSystem.setShaderFogStart(-8);
			RenderSystem.setShaderFogEnd(5);
			ci.cancel();
		}
	}
}
