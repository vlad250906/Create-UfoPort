package com.simibubi.create.content.contraptions;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class TrainCollisionPacket extends SimplePacketBase {

	int damage;
	int contraptionEntityId;

	public TrainCollisionPacket(int damage, int contraptionEntityId) {
		this.damage = damage;
		this.contraptionEntityId = contraptionEntityId;
	}

	public TrainCollisionPacket(RegistryFriendlyByteBuf buffer) {
		contraptionEntityId = buffer.readInt();
		damage = buffer.readInt();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeInt(contraptionEntityId);
		buffer.writeInt(damage);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			Level level = player.level();

			Entity entity = level.getEntity(contraptionEntityId);
			if (!(entity instanceof CarriageContraptionEntity cce))
				return;

			player.hurt(CreateDamageSources.runOver(level, cce), damage);
			player.level().playSound(player, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.NEUTRAL,
				1, .75f);
		});
		return true;
	}

}
