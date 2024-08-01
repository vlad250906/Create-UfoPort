package com.simibubi.create.content.kinetics.fan;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class AirFlowParticleData implements ParticleOptions, ICustomParticleDataWithSprite<AirFlowParticleData> {

	public static final MapCodec<AirFlowParticleData> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(Codec.INT.fieldOf("x").forGetter(p -> p.posX), Codec.INT.fieldOf("y").forGetter(p -> p.posY),
					Codec.INT.fieldOf("z").forGetter(p -> p.posZ)).apply(i, AirFlowParticleData::new));
	
	public static final StreamCodec<RegistryFriendlyByteBuf, AirFlowParticleData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT, obj -> obj.posX,
		ByteBufCodecs.INT, obj -> obj.posY,
		ByteBufCodecs.INT, obj -> obj.posZ,
		AirFlowParticleData::new
	);

//	public static final ParticleOptions.Deserializer<AirFlowParticleData> DESERIALIZER = new ParticleOptions.Deserializer<AirFlowParticleData>() {
//		public AirFlowParticleData fromCommand(ParticleType<AirFlowParticleData> particleTypeIn, StringReader reader)
//				throws CommandSyntaxException {
//			reader.expect(' ');
//			int x = reader.readInt();
//			reader.expect(' ');
//			int y = reader.readInt();
//			reader.expect(' ');
//			int z = reader.readInt();
//			return new AirFlowParticleData(x, y, z);
//		}
//
//		public AirFlowParticleData fromNetwork(ParticleType<AirFlowParticleData> particleTypeIn,
//				FriendlyByteBuf buffer) {
//			return new AirFlowParticleData(buffer.readInt(), buffer.readInt(), buffer.readInt());
//		}
//	};

	final int posX;
	final int posY;
	final int posZ;

	public AirFlowParticleData(Vec3i pos) {
		this(pos.getX(), pos.getY(), pos.getZ());
	}

	public AirFlowParticleData(int posX, int posY, int posZ) {
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
	}

	public AirFlowParticleData() {
		this(0, 0, 0);
	}

	@Override
	public ParticleType<?> getType() {
		return AllParticleTypes.AIR_FLOW.get();
	}

//	@Override
//	public void writeToNetwork(FriendlyByteBuf buffer) {
//		buffer.writeInt(posX);
//		buffer.writeInt(posY);
//		buffer.writeInt(posZ);
//	}
//
//	@Override
//	public String writeToString() {
//		return String.format(Locale.ROOT, "%s %d %d %d", AllParticleTypes.AIR_FLOW.parameter(), posX, posY, posZ);
//	}
//
//	@Override
//	public Deserializer<AirFlowParticleData> getDeserializer() {
//		return DESERIALIZER;
//	}

	@Override
	public MapCodec<AirFlowParticleData> getCodec(ParticleType<AirFlowParticleData> type) {
		return CODEC;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public SpriteParticleRegistration<AirFlowParticleData> getMetaFactory() {
		return AirFlowParticle.Factory::new;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, AirFlowParticleData> getStreamCodec(
			ParticleType<AirFlowParticleData> type) {
		return STREAM_CODEC;
	}

}