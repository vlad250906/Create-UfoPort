package com.simibubi.create.content.fluids.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.foundation.particle.ICustomParticleData;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluids;

public class FluidParticleData implements ParticleOptions, ICustomParticleData<FluidParticleData> {

	private ParticleType<FluidParticleData> type;
	private FluidStack fluid;

	public FluidParticleData() {}

	@SuppressWarnings("unchecked")
	public FluidParticleData(ParticleType<?> type, FluidStack fluid) {
		this.type = (ParticleType<FluidParticleData>) type;
		this.fluid = fluid;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ParticleProvider<FluidParticleData> getFactory() {
		return this::create;
	}

	// fabric: lambda funk
	@Environment(EnvType.CLIENT)
	private Particle create(FluidParticleData type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		return FluidStackParticle.create(type.type, level, type.fluid, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	@Override
	public ParticleType<?> getType() {
		return type;
	}

//	@Override
//	public void writeToNetwork(FriendlyByteBuf buffer) {
//		fluid.writeToPacket(buffer);
//	}

//	@Override
//	public String writeToString() {
//		return RegisteredObjects.getKeyOrThrow(type) + " " + RegisteredObjects.getKeyOrThrow(fluid.getFluid());
//	}

	public static final MapCodec<FluidParticleData> CODEC = RecordCodecBuilder.mapCodec(i -> i
		.group(FluidStack.CODEC.fieldOf("fluid")
			.forGetter(p -> p.fluid))
		.apply(i, fs -> new FluidParticleData(AllParticleTypes.FLUID_PARTICLE.get(), fs)));

	public static final MapCodec<FluidParticleData> BASIN_CODEC = RecordCodecBuilder.mapCodec(i -> i
		.group(FluidStack.CODEC.fieldOf("fluid")
			.forGetter(p -> p.fluid))
		.apply(i, fs -> new FluidParticleData(AllParticleTypes.BASIN_FLUID.get(), fs)));

	public static final MapCodec<FluidParticleData> DRIP_CODEC = RecordCodecBuilder.mapCodec(i -> i
		.group(FluidStack.CODEC.fieldOf("fluid")
			.forGetter(p -> p.fluid))
		.apply(i, fs -> new FluidParticleData(AllParticleTypes.FLUID_DRIP.get(), fs)));
	
	public static final StreamCodec<RegistryFriendlyByteBuf, FluidParticleData> STREAM_CODEC = StreamCodec.composite(
			FluidStack.STREAM_CODEC, p -> p.fluid, 
			p -> new FluidParticleData(AllParticleTypes.FLUID_PARTICLE.get(), p)
	);
	
	public static final StreamCodec<RegistryFriendlyByteBuf, FluidParticleData> BASIN_STREAM_CODEC = StreamCodec.composite(
			FluidStack.STREAM_CODEC, p -> p.fluid, 
			p -> new FluidParticleData(AllParticleTypes.BASIN_FLUID.get(), p)
	);
	
	public static final StreamCodec<RegistryFriendlyByteBuf, FluidParticleData> DRIP_STREAM_CODEC = StreamCodec.composite(
			FluidStack.STREAM_CODEC, p -> p.fluid, 
			p -> new FluidParticleData(AllParticleTypes.FLUID_DRIP.get(), p)
	);
	
//	public static final ParticleOptions.Deserializer<FluidParticleData> DESERIALIZER =
//		new ParticleOptions.Deserializer<FluidParticleData>() {
//
//			// TODO Fluid particles on command
//			public FluidParticleData fromCommand(ParticleType<FluidParticleData> particleTypeIn, StringReader reader)
//				throws CommandSyntaxException {
//				return new FluidParticleData(particleTypeIn, new FluidStack(Fluids.WATER, 1));
//			}
//
//			public FluidParticleData fromNetwork(ParticleType<FluidParticleData> particleTypeIn, FriendlyByteBuf buffer) {
//				return new FluidParticleData(particleTypeIn, FluidStack.readFromPacket(buffer));
//			}
//		};
//
//	@Override
//	public Deserializer<FluidParticleData> getDeserializer() {
//		return DESERIALIZER;
//	}

	@Override
	public MapCodec<FluidParticleData> getCodec(ParticleType<FluidParticleData> type) {
		if (type == AllParticleTypes.BASIN_FLUID.get())
			return BASIN_CODEC;
		if (type == AllParticleTypes.FLUID_DRIP.get())
			return DRIP_CODEC;
		return CODEC;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, FluidParticleData> getStreamCodec(
			ParticleType<FluidParticleData> type) {
		if (type == AllParticleTypes.BASIN_FLUID.get())
			return BASIN_STREAM_CODEC;
		if (type == AllParticleTypes.FLUID_DRIP.get())
			return DRIP_STREAM_CODEC;
		return STREAM_CODEC;
	}

}
