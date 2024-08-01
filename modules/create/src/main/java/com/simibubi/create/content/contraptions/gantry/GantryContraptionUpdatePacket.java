package com.simibubi.create.content.contraptions.gantry;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class GantryContraptionUpdatePacket extends SimplePacketBase {

	int entityID;
	double coord;
	double motion;
	double sequenceLimit;

	public GantryContraptionUpdatePacket(int entityID, double coord, double motion, double sequenceLimit) {
		this.entityID = entityID;
		this.coord = coord;
		this.motion = motion;
		this.sequenceLimit = sequenceLimit;
	}

	public GantryContraptionUpdatePacket(RegistryFriendlyByteBuf buffer) {
		entityID = buffer.readInt();
		coord = buffer.readFloat();
		motion = buffer.readFloat();
		sequenceLimit = buffer.readFloat();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeInt(entityID);
		buffer.writeFloat((float) coord);
		buffer.writeFloat((float) motion);
		buffer.writeFloat((float) sequenceLimit);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(
			() -> EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> GantryContraptionEntity.handlePacket(this)));
		return true;
	}

}
