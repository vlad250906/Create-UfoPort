package com.tterrag.registrate.fabric;

import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.resources.ResourceLocation;

public record FluidData(String translationKey, int light) {

	public interface RenderHandlerFactory {
		Object create(ResourceLocation stillTexture, ResourceLocation flowingTexture);
	}

	public static RenderHandlerFactory createDefaultHandler() {
		return (stillTexture, flowingTexture) -> {
			final SimpleFluidRenderHandler handler = new SimpleFluidRenderHandler(stillTexture, flowingTexture,
					flowingTexture, -1);
			return handler;
		};
	}

	public static <T extends SimpleFlowableFluid> void registerRenderHandler(
			NonNullSupplier<RenderHandlerFactory> renderHandler, T entry, ResourceLocation stillTexture,
			ResourceLocation flowingTexture) {
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			final FluidRenderHandler handler = (FluidRenderHandler) renderHandler.get().create(stillTexture,
					flowingTexture);
			FluidRenderHandlerRegistry.INSTANCE.register(entry, handler);
			FluidRenderHandlerRegistry.INSTANCE.register(entry.getSource(), handler);
		});
	}
}
