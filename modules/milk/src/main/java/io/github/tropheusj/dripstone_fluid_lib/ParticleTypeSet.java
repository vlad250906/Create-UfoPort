package io.github.tropheusj.dripstone_fluid_lib;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;

public record ParticleTypeSet(ParticleOptions hang, ParticleOptions fall, ParticleOptions splash) {
}
