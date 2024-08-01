package io.github.tropheusj.dripstone_fluid_lib.mixin;

import io.github.tropheusj.dripstone_fluid_lib.Constants;
import io.github.tropheusj.dripstone_fluid_lib.ParticleFactories.DrippingDripstoneFluidFactory;
import io.github.tropheusj.dripstone_fluid_lib.ParticleFactories.DripstoneFluidSplashFactory;
import io.github.tropheusj.dripstone_fluid_lib.ParticleFactories.FallingDripstoneFluidFactory;
import io.github.tropheusj.dripstone_fluid_lib.ParticleTypeSet;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
	@Shadow
	@Final
	public ParticleEngine particleEngine;

	@Inject(at = @At(value = "INVOKE", shift = At.Shift.BY, by = 2, target = "Lnet/minecraft/client/particle/ParticleEngine;<init>(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/renderer/texture/TextureManager;)V"), method = "<init>")
	private void dripstone_fluid_lib$handleParticles(GameConfig args, CallbackInfo ci) {
		Constants.TO_REGISTER.forEach(fluid -> {
			ParticleTypeSet particles = Constants.FLUIDS_TO_PARTICLES.get(fluid);
			ParticleManagerAccessor access = (ParticleManagerAccessor) particleEngine;
			//ParticleEngine.SpriteParticleRegistration<ParticleOptions> govno = (SpriteParticleRegistration<ParticleOptions>) new DripstoneFluidSplashFactory((SpriteSet)prov, fluid);
			//access.register((ParticleType<ParticleOptions>)particles.hang().getType(), (ParticleProvider.Sprite<ParticleOptions>)new DrippingDripstoneFluidFactory(fluid));
			//access.register((ParticleType<ParticleOptions>)particles.fall(), (ParticleProvider.Sprite<ParticleOptions>)new FallingDripstoneFluidFactory(fluid));
			//access.register((ParticleType<ParticleOptions>)particles.splash(), (ParticleEngine.SpriteParticleRegistration<ParticleOptions>)(fluid));
		});
	}
}
