package com.tterrag.registrate.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;

import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class ClientInit implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(FluidSpriteReloadListener.INSTANCE);
//		MixinEnvironment.getCurrentEnvironment().audit();
	}
}
