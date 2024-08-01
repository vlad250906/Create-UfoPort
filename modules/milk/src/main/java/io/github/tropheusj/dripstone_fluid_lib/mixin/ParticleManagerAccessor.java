package io.github.tropheusj.dripstone_fluid_lib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

@Environment(EnvType.CLIENT)
@Mixin(ParticleEngine.class)
public interface ParticleManagerAccessor {
	//@Invoker
	//<T extends ParticleOptions> void register(
			//ParticleType<T> type, ParticleEngine.SpriteParticleRegistration<T> factory
	//);

	//@Invoker
	//<T extends ParticleOptions> void register(
			//ParticleType<T> type, ParticleProvider.Sprite<T> factory
	//);
}
