package com.simibubi.create.foundation.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface ICustomParticleData<T extends ParticleOptions> {

	//Deserializer<T> getDeserializer();

	MapCodec<T> getCodec(ParticleType<T> type);
	StreamCodec<RegistryFriendlyByteBuf, T> getStreamCodec(ParticleType<T> type);

	public default ParticleType<T> createType() {
		return new ParticleType<T>(false) {

			@Override
			public MapCodec<T> codec() {
				return ICustomParticleData.this.getCodec(this);
			}

			@Override
			public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
				return ICustomParticleData.this.getStreamCodec(this);
			}
		};
	}

	@Environment(EnvType.CLIENT)
	public ParticleProvider<T> getFactory();

	@Environment(EnvType.CLIENT)
	public default void register(ParticleType<T> type, ParticleEngine particles) {
		ParticleFactoryRegistry.getInstance().register(type, getFactory());
	}

}
