package com.simibubi.create.content.kinetics.base;

import java.util.Locale;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class RotationIndicatorParticleData
	implements ParticleOptions, ICustomParticleDataWithSprite<RotationIndicatorParticleData> {

	// TODO 1.16 make this unnecessary
	public static final PrimitiveCodec<Character> CHAR = new PrimitiveCodec<Character>() {
		@Override
		public <T> DataResult<Character> read(final DynamicOps<T> ops, final T input) {
			return ops.getNumberValue(input)
				.map(n -> (char) n.shortValue());
		}

		@Override
		public <T> T write(final DynamicOps<T> ops, final Character value) {
			return ops.createShort((short) value.charValue());
		}

		@Override
		public String toString() {
			return "Char";
		}
	};

	public static final MapCodec<RotationIndicatorParticleData> CODEC = RecordCodecBuilder.mapCodec(i -> i
		.group(Codec.INT.fieldOf("color")
			.forGetter(p -> p.color),
			Codec.FLOAT.fieldOf("speed")
				.forGetter(p -> p.speed),
			Codec.FLOAT.fieldOf("radius1")
				.forGetter(p -> p.radius1),
			Codec.FLOAT.fieldOf("radius2")
				.forGetter(p -> p.radius2),
			Codec.INT.fieldOf("lifeSpan")
				.forGetter(p -> p.lifeSpan),
			CHAR.fieldOf("axis")
				.forGetter(p -> p.axis))
		.apply(i, RotationIndicatorParticleData::new));
	
	public static final StreamCodec<RegistryFriendlyByteBuf, RotationIndicatorParticleData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT, obj -> obj.color,
		ByteBufCodecs.FLOAT, obj -> obj.speed,
		ByteBufCodecs.FLOAT, obj -> obj.radius1,
		ByteBufCodecs.FLOAT, obj -> obj.radius2,
		ByteBufCodecs.INT, obj -> obj.lifeSpan,
		ByteBufCodecs.INT, obj -> (int)obj.axis,
		(a1, a2, a3, a4, a5, a6) -> new RotationIndicatorParticleData(a1, a2, a3, a4, a5, (char)(a6.intValue()))
	);

//	public static final ParticleOptions.Deserializer<RotationIndicatorParticleData> DESERIALIZER =
//		new ParticleOptions.Deserializer<RotationIndicatorParticleData>() {
//			public RotationIndicatorParticleData fromCommand(ParticleType<RotationIndicatorParticleData> particleTypeIn,
//				StringReader reader) throws CommandSyntaxException {
//				reader.expect(' ');
//				int color = reader.readInt();
//				reader.expect(' ');
//				float speed = (float) reader.readDouble();
//				reader.expect(' ');
//				float rad1 = (float) reader.readDouble();
//				reader.expect(' ');
//				float rad2 = (float) reader.readDouble();
//				reader.expect(' ');
//				int lifeSpan = reader.readInt();
//				reader.expect(' ');
//				char axis = reader.read();
//				return new RotationIndicatorParticleData(color, speed, rad1, rad2, lifeSpan, axis);
//			}
//
//			public RotationIndicatorParticleData fromNetwork(ParticleType<RotationIndicatorParticleData> particleTypeIn,
//				FriendlyByteBuf buffer) {
//				return new RotationIndicatorParticleData(buffer.readInt(), buffer.readFloat(), buffer.readFloat(),
//					buffer.readFloat(), buffer.readInt(), buffer.readChar());
//			}
//		};

	final int color;
	final float speed;
	final float radius1;
	final float radius2;
	final int lifeSpan;
	final char axis;

	public RotationIndicatorParticleData(int color, float speed, float radius1, float radius2, int lifeSpan,
		char axis) {
		this.color = color;
		this.speed = speed;
		this.radius1 = radius1;
		this.radius2 = radius2;
		this.lifeSpan = lifeSpan;
		this.axis = axis;
	}

	public RotationIndicatorParticleData() {
		this(0, 0, 0, 0, 0, '0');
	}

	@Override
	public ParticleType<?> getType() {
		return AllParticleTypes.ROTATION_INDICATOR.get();
	}

	public Axis getAxis() {
		return Axis.valueOf(axis + "");
	}

//	@Override
//	public void writeToNetwork(FriendlyByteBuf buffer) {
//		buffer.writeInt(color);
//		buffer.writeFloat(speed);
//		buffer.writeFloat(radius1);
//		buffer.writeFloat(radius2);
//		buffer.writeInt(lifeSpan);
//		buffer.writeChar(axis);
//	}

	@Override
	public MapCodec<RotationIndicatorParticleData> getCodec(ParticleType<RotationIndicatorParticleData> type) {
		return CODEC;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public SpriteParticleRegistration<RotationIndicatorParticleData> getMetaFactory() {
		return RotationIndicatorParticle.Factory::new;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, RotationIndicatorParticleData> getStreamCodec(
			ParticleType<RotationIndicatorParticleData> type) {
		return STREAM_CODEC;
	}

}