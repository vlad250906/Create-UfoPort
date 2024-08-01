package com.simibubi.create.content.trains.graph;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.RegistryFriendlyByteBuf;

public class TrackGraphRequestPacket extends SimplePacketBase {

	private int netId;

	public TrackGraphRequestPacket(int netId) {
		this.netId = netId;
	}

	public TrackGraphRequestPacket(RegistryFriendlyByteBuf buffer) {
		netId = buffer.readInt();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeInt(netId);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
				for (TrackGraph trackGraph : Create.RAILWAYS.trackNetworks.values()) {
				if (trackGraph.netId == netId) {
					Create.RAILWAYS.sync.sendFullGraphTo(trackGraph, context.getSender());
					break;
				}
			}
		});
		return true;
	}

}
