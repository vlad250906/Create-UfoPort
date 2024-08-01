package com.simibubi.create.foundation.blockEntity;

import com.simibubi.create.foundation.networking.BlockEntityDataPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class RemoveBlockEntityPacket extends BlockEntityDataPacket<SyncedBlockEntity> {

	public RemoveBlockEntityPacket(BlockPos pos) {
		super(pos);
	}

	public RemoveBlockEntityPacket(RegistryFriendlyByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeData(RegistryFriendlyByteBuf buffer) {}

	@Override
	protected void handlePacket(SyncedBlockEntity be) {
		if (!be.hasLevel()) {
			be.setRemoved();
			return;
		}

		be.getLevel()
			.removeBlockEntity(pos);
	}

}
