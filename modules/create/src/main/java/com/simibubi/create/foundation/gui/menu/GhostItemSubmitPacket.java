package com.simibubi.create.foundation.gui.menu;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class GhostItemSubmitPacket extends SimplePacketBase {

	private final ItemStack item;
	private final int slot;

	public GhostItemSubmitPacket(ItemStack item, int slot) {
		this.item = item;
		this.slot = slot;
	}

	public GhostItemSubmitPacket(RegistryFriendlyByteBuf buffer) {
		item = ItemStack.STREAM_CODEC.decode(buffer);
		//item = buffer.readItem();
		slot = buffer.readInt();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		ItemStack.STREAM_CODEC.encode(buffer, item);
		//buffer.writeItem(item);
		buffer.writeInt(slot);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null)
				return;

			if (player.containerMenu instanceof GhostItemMenu<?> menu) {
				menu.ghostInventory.setStackInSlot(slot, item);
				menu.getSlot(36 + slot).setChanged();
			}
		});
		return true;
	}

}
