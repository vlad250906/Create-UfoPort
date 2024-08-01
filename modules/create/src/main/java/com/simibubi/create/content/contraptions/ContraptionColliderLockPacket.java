package com.simibubi.create.content.contraptions;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class ContraptionColliderLockPacket extends SimplePacketBase {

	protected int contraption;
	protected double offset;
	private int sender;

	public ContraptionColliderLockPacket(int contraption, double offset, int sender) {
		this.contraption = contraption;
		this.offset = offset;
		this.sender = sender;
	}

	public ContraptionColliderLockPacket(RegistryFriendlyByteBuf buffer) {
		contraption = buffer.readVarInt();
		offset = buffer.readDouble();
		sender = buffer.readVarInt();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeVarInt(contraption);
		buffer.writeDouble(offset);
		buffer.writeVarInt(sender);
	}

	@Override
	public boolean handle(Context context) {
		EnvExecutor.runWhenOn(EnvType.CLIENT,
			() -> () -> ContraptionCollider.lockPacketReceived(contraption, sender, offset));
		return true;
	}

	public static class ContraptionColliderLockPacketRequest extends SimplePacketBase {

		protected int contraption;
		protected double offset;

		public ContraptionColliderLockPacketRequest(int contraption, double offset) {
			this.contraption = contraption;
			this.offset = offset;
		}

		public ContraptionColliderLockPacketRequest(RegistryFriendlyByteBuf buffer) {
			contraption = buffer.readVarInt();
			offset = buffer.readDouble();
		}

		@Override
		public void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeVarInt(contraption);
			buffer.writeDouble(offset);
		}

		@Override
		public boolean handle(Context context) {
			context.enqueueWork(() -> {
				AllPackets.getChannel()
					.sendToClientsTracking(
							new ContraptionColliderLockPacket(contraption, offset, context.getSender().getId()),
							context.getSender()
					);
			});
			return true;
		}

	}

}
