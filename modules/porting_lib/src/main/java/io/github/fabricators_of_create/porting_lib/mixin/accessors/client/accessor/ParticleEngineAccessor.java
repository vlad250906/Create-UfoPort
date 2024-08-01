package io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;

@Environment(EnvType.CLIENT)
@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
	@Accessor("providers")
	Int2ObjectMap<ParticleProvider<?>> port_lib$getProviders();
}
