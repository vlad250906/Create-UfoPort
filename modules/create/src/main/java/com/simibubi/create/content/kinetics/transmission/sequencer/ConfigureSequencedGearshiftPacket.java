package com.simibubi.create.content.kinetics.transmission.sequencer;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class ConfigureSequencedGearshiftPacket extends BlockEntityConfigurationPacket<SequencedGearshiftBlockEntity> {

	private ListTag instructions;

	public ConfigureSequencedGearshiftPacket(BlockPos pos, ListTag instructions) {
		super(pos);
		this.instructions = instructions;
	}

	public ConfigureSequencedGearshiftPacket(RegistryFriendlyByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void readSettings(RegistryFriendlyByteBuf buffer) {
		instructions = buffer.readNbt().getList("data", Tag.TAG_COMPOUND);
	}

	@Override
	protected void writeSettings(RegistryFriendlyByteBuf buffer) {
		CompoundTag tag = new CompoundTag();
		tag.put("data", instructions);
		buffer.writeNbt(tag);
	}

	@Override
	protected void applySettings(SequencedGearshiftBlockEntity be) {

		be.run(-1);
		be.instructions = Instruction.deserializeAll(instructions);
		be.sendData();
	}

}
