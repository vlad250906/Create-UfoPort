package com.simibubi.create.content.contraptions.actors.trainControls;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.RegistryFriendlyByteBuf;

public class ControlsStopControllingPacket extends SimplePacketBase {

	public ControlsStopControllingPacket() {}

	public ControlsStopControllingPacket(RegistryFriendlyByteBuf buffer) {}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(ControlsHandler::stopControlling);
		return true;
	}

}
