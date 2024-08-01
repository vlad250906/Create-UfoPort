package io.github.tropheusj.dripstone_fluid_lib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.SplashParticle;
import net.minecraft.client.particle.SpriteSet;

@Environment(EnvType.CLIENT)
@Mixin(SplashParticle.Provider.class)
public interface WaterSplashParticle$SplashFactoryAccessor {
	@Accessor("sprite")
	SpriteSet dripstone_fluid_lib$SpriteSet();
}
