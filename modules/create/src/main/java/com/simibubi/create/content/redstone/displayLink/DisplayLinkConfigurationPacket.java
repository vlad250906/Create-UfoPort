package com.simibubi.create.content.redstone.displayLink;

import com.simibubi.create.content.redstone.displayLink.source.DisplaySource;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class DisplayLinkConfigurationPacket extends BlockEntityConfigurationPacket<DisplayLinkBlockEntity> {

	private CompoundTag configData;
	private int targetLine;

	public DisplayLinkConfigurationPacket(BlockPos pos, CompoundTag configData, int targetLine) {
		super(pos);
		this.configData = configData;
		this.targetLine = targetLine;
	}

	public DisplayLinkConfigurationPacket(RegistryFriendlyByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeSettings(RegistryFriendlyByteBuf buffer) {
		buffer.writeNbt(configData);
		buffer.writeInt(targetLine);
	}

	@Override
	protected void readSettings(RegistryFriendlyByteBuf buffer) {
		configData = buffer.readNbt();
		targetLine = buffer.readInt();
	}

	@Override
	protected void applySettings(DisplayLinkBlockEntity be) {
		be.targetLine = targetLine;

		if (!configData.contains("Id")) {
			be.notifyUpdate();
			return;
		}

		ResourceLocation id = ResourceLocation.parse(configData.getString("Id"));
		DisplaySource source = AllDisplayBehaviours.getSource(id);
		if (source == null) {
			be.notifyUpdate();
			return;
		}

		if (be.activeSource == null || be.activeSource != source) {
			be.activeSource = source;
			be.setSourceConfig(configData.copy());
		} else {
			be.getSourceConfig()
				.merge(configData);
		}

		be.updateGatheredData();
		be.notifyUpdate();
	}

}
