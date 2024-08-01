package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class EjectorTriggerPacket extends BlockEntityConfigurationPacket<EjectorBlockEntity> {

	public EjectorTriggerPacket(BlockPos pos) {
		super(pos);
	}
	
	public EjectorTriggerPacket(RegistryFriendlyByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeSettings(RegistryFriendlyByteBuf buffer) {}

	@Override
	protected void readSettings(RegistryFriendlyByteBuf buffer) {}

	@Override
	protected void applySettings(EjectorBlockEntity be) {
		be.activate();
	}

}
