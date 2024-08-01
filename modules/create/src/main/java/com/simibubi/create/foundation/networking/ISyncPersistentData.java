package com.simibubi.create.foundation.networking;

import java.util.HashSet;

import com.simibubi.create.AllPackets;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;

public interface ISyncPersistentData {

	void onPersistentDataUpdated();

	default void syncPersistentDataWithTracking(Entity self) {
		AllPackets.getChannel().sendToClientsTracking(new PersistentDataPacket(self), self);
	}

	public static class PersistentDataPacket extends SimplePacketBase {

		private int entityId;
		private Entity entity;
		private CompoundTag readData;

		public PersistentDataPacket(Entity entity) {
			this.entity = entity;
			this.entityId = entity.getId();
		}

		public PersistentDataPacket(RegistryFriendlyByteBuf buffer) {
			entityId = buffer.readInt();
			readData = buffer.readNbt();
		}

		@Override
		public void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeInt(entityId);
			buffer.writeNbt(entity.getCustomData());
		}

		@Override
		public boolean handle(Context context) {
			context.enqueueWork(() -> {
				Entity entityByID = Minecraft.getInstance().level.getEntity(entityId);
				CompoundTag data = entityByID.getCustomData();
				new HashSet<>(data.getAllKeys()).forEach(data::remove);
				data.merge(readData);
				if (!(entityByID instanceof ISyncPersistentData))
					return;
				((ISyncPersistentData) entityByID).onPersistentDataUpdated();
			});
			return true;
		}

	}

}
