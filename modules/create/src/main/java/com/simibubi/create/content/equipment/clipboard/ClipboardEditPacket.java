package com.simibubi.create.content.equipment.clipboard;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ClipboardEditPacket extends SimplePacketBase {

	private int hotbarSlot;
	private CompoundTag data;
	private BlockPos targetedBlock;

	public ClipboardEditPacket(int hotbarSlot, CompoundTag data, @Nullable BlockPos targetedBlock) {
		this.hotbarSlot = hotbarSlot;
		this.data = data;
		this.targetedBlock = targetedBlock;
	}

	public ClipboardEditPacket(RegistryFriendlyByteBuf buffer) {
		hotbarSlot = buffer.readVarInt();
		data = buffer.readNbt();
		if (buffer.readBoolean())
			targetedBlock = buffer.readBlockPos();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeVarInt(hotbarSlot);
		buffer.writeNbt(data);
		buffer.writeBoolean(targetedBlock != null);
		if (targetedBlock != null)
			buffer.writeBlockPos(targetedBlock);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer sender = context.getSender();

			if (targetedBlock != null) {
				Level world = sender.level();
				if (world == null || !world.isLoaded(targetedBlock))
					return;
				if (!targetedBlock.closerThan(sender.blockPosition(), 20))
					return;
				if (world.getBlockEntity(targetedBlock) instanceof ClipboardBlockEntity cbe) {
					cbe.dataContainer.set(AllDataComponents.CLIPBOARD_EDITING, data.isEmpty() ? null : data);
					cbe.onEditedBy(sender);
				}
				return;
			}

			ItemStack itemStack = sender.getInventory()
				.getItem(hotbarSlot);
			if (!AllBlocks.CLIPBOARD.isIn(itemStack))
				return;
			itemStack.set(AllDataComponents.CLIPBOARD_EDITING, data.isEmpty() ? null : data);
		});

		return true;
	}

}
