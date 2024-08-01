package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public abstract class ShootGadgetPacket extends SimplePacketBase {

	public Vec3 location;
	public InteractionHand hand;
	public boolean self;

	public ShootGadgetPacket(Vec3 location, InteractionHand hand, boolean self) {
		this.location = location;
		this.hand = hand;
		this.self = self;
	}

	public ShootGadgetPacket(RegistryFriendlyByteBuf buffer) {
		hand = buffer.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		self = buffer.readBoolean();
		location = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		readAdditional(buffer);
	}

	public final void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBoolean(hand == InteractionHand.MAIN_HAND);
		buffer.writeBoolean(self);
		buffer.writeDouble(location.x);
		buffer.writeDouble(location.y);
		buffer.writeDouble(location.z);
		writeAdditional(buffer);
	}

	protected abstract void readAdditional(RegistryFriendlyByteBuf buffer);

	protected abstract void writeAdditional(RegistryFriendlyByteBuf buffer);

	@Environment(EnvType.CLIENT)
	protected abstract void handleAdditional();

	@Environment(EnvType.CLIENT)
	protected abstract ShootableGadgetRenderHandler getHandler();

	@Override
	@Environment(EnvType.CLIENT)
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			Entity renderViewEntity = Minecraft.getInstance()
				.getCameraEntity();
			if (renderViewEntity == null)
				return;
			if (renderViewEntity.position()
				.distanceTo(location) > 100)
				return;

			ShootableGadgetRenderHandler handler = getHandler();
			handleAdditional();
			if (self)
				handler.shoot(hand, location);
			else
				handler.playSound(hand, location);
		});
		return true;
	}

}
