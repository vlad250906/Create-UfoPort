package com.simibubi.create.content.contraptions;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;

public class ContraptionBlockChangedPacket extends SimplePacketBase {

	int entityID;
	BlockPos localPos;
	BlockState newState;

	public ContraptionBlockChangedPacket(int id, BlockPos pos, BlockState state) {
		entityID = id;
		localPos = pos;
		newState = state;
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeInt(entityID);
		buffer.writeBlockPos(localPos);
		buffer.writeNbt(NbtUtils.writeBlockState(newState));
	}

	@SuppressWarnings("deprecation")
	public ContraptionBlockChangedPacket(RegistryFriendlyByteBuf buffer) {
		entityID = buffer.readInt();
		localPos = buffer.readBlockPos();
		newState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), buffer.readNbt());
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> EnvExecutor.runWhenOn(EnvType.CLIENT,
			() -> () -> AbstractContraptionEntity.handleBlockChangedPacket(this)));
		return true;
	}

}
