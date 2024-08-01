package com.simibubi.create.content.contraptions.glue;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.AdventureUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class SuperGlueRemovalPacket extends SimplePacketBase {

	private int entityId;
	private BlockPos soundSource;

	public SuperGlueRemovalPacket(int id, BlockPos soundSource) {
		entityId = id;
		this.soundSource = soundSource;
	}

	public SuperGlueRemovalPacket(RegistryFriendlyByteBuf buffer) {
		entityId = buffer.readInt();
		soundSource = buffer.readBlockPos();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeInt(entityId);
		buffer.writeBlockPos(soundSource);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (AdventureUtil.isAdventure(player))
				return;
			Entity entity = player.level().getEntity(entityId);
			if (!(entity instanceof SuperGlueEntity superGlue))
				return;
			double range = 32;
			if (player.distanceToSqr(superGlue.position()) > range * range)
				return;
			AllSoundEvents.SLIME_ADDED.play(player.level(), null, soundSource, 0.5F, 0.5F);
			superGlue.spawnParticles();
			entity.discard();
		});
		return true;
	}

}
