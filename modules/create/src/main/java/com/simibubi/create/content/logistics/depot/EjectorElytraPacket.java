package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;


public class EjectorElytraPacket extends SimplePacketBase {

	private BlockPos pos;

	public EjectorElytraPacket(BlockPos pos) {
		this.pos = pos;
	}

	public EjectorElytraPacket(RegistryFriendlyByteBuf buffer) {
		pos = buffer.readBlockPos();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(pos);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
					ServerPlayer player = context.getSender();
					if (player == null)
						return;
					Level world = player.level();
					if (world == null || !world.isLoaded(pos))
						return;
					BlockEntity blockEntity = world.getBlockEntity(pos);
					if (blockEntity instanceof EjectorBlockEntity)
						((EjectorBlockEntity) blockEntity).deployElytra(player);
				});
		return true;
	}

}
