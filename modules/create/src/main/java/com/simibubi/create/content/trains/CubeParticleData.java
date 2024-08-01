package com.simibubi.create.content.trains;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.foundation.particle.ICustomParticleData;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class CubeParticleData implements ParticleOptions, ICustomParticleData<CubeParticleData> {

	public static final MapCodec<CubeParticleData> CODEC = RecordCodecBuilder.mapCodec(i -> 
		i.group(
			Codec.FLOAT.fieldOf("r").forGetter(p -> p.r),
			Codec.FLOAT.fieldOf("g").forGetter(p -> p.g),
			Codec.FLOAT.fieldOf("b").forGetter(p -> p.b),
			Codec.FLOAT.fieldOf("scale").forGetter(p -> p.scale),
			Codec.INT.fieldOf("avgAge").forGetter(p -> p.avgAge),
			Codec.BOOL.fieldOf("hot").forGetter(p -> p.hot))
		.apply(i, CubeParticleData::new));
	
	public static final StreamCodec<RegistryFriendlyByteBuf, CubeParticleData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT, p -> p.r,
		ByteBufCodecs.FLOAT, p -> p.g,
		ByteBufCodecs.FLOAT, p -> p.b,
		ByteBufCodecs.FLOAT, p -> p.scale,
		ByteBufCodecs.INT, p -> p.avgAge,
		ByteBufCodecs.BOOL, p -> p.hot,
		CubeParticleData::new
	);

//	public static final ParticleOptions.Deserializer<CubeParticleData> DESERIALIZER = new ParticleOptions.Deserializer<CubeParticleData>() {
//		@Override
//		public CubeParticleData fromCommand(ParticleType<CubeParticleData> type, StringReader reader) throws CommandSyntaxException {
//			reader.expect(' ');
//			float r = reader.readFloat();
//			reader.expect(' ');
//			float g = reader.readFloat();
//			reader.expect(' ');
//			float b = reader.readFloat();
//			reader.expect(' ');
//			float scale = reader.readFloat();
//			reader.expect(' ');
//			int avgAge = reader.readInt();
//			reader.expect(' ');
//			boolean hot = reader.readBoolean();
//			return new CubeParticleData(r, g, b, scale, avgAge, hot);
//		}
//
//		@Override
//		public CubeParticleData fromNetwork(ParticleType<CubeParticleData> type, FriendlyByteBuf buffer) {
//			return new CubeParticleData(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readInt(), buffer.readBoolean());
//		}
//	};

	final float r;
	final float g;
	final float b;
	final float scale;
	final int avgAge;
	final boolean hot;

	public CubeParticleData(float r, float g, float b, float scale, int avgAge, boolean hot) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.scale = scale;
		this.avgAge = avgAge;
		this.hot = hot;
	}

	public CubeParticleData() {
		this(0, 0, 0, 0, 0, false);
	}

//	@Override
//	public Deserializer<CubeParticleData> getDeserializer() {
//		return DESERIALIZER;
//	}

	@Override
	public MapCodec<CubeParticleData> getCodec(ParticleType<CubeParticleData> type) {
		return CODEC;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ParticleProvider<CubeParticleData> getFactory() {
		return new CubeParticle.Factory();
	}

	@Override
	public ParticleType<?> getType() {
		return AllParticleTypes.CUBE.get();
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, CubeParticleData> getStreamCodec(ParticleType<CubeParticleData> type) {
		return STREAM_CODEC;
	}

//	@Override
//	public void writeToNetwork(FriendlyByteBuf buffer) {
//		buffer.writeFloat(r);
//		buffer.writeFloat(g);
//		buffer.writeFloat(b);
//		buffer.writeFloat(scale);
//		buffer.writeInt(avgAge);
//		buffer.writeBoolean(hot);
//	}
//
//	@Override
//	public String writeToString() {
//		return String.format(Locale.ROOT, "%s %f %f %f %f %d %s", AllParticleTypes.CUBE.parameter(), r, g, b, scale, avgAge, hot);
//	}
}
