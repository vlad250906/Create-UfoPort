package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.zapper.ZapperRenderHandler.LaserBeam;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

public class ZapperBeamPacket extends ShootGadgetPacket {

	public Vec3 target;

	public ZapperBeamPacket(Vec3 start, Vec3 target, InteractionHand hand, boolean self) {
		super(start, hand, self);
		this.target = target;
	}

	public ZapperBeamPacket(RegistryFriendlyByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void readAdditional(RegistryFriendlyByteBuf buffer) {
		target = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
	}

	@Override
	protected void writeAdditional(RegistryFriendlyByteBuf buffer) {
		buffer.writeDouble(target.x);
		buffer.writeDouble(target.y);
		buffer.writeDouble(target.z);
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected ShootableGadgetRenderHandler getHandler() {
		return CreateClient.ZAPPER_RENDER_HANDLER;
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected void handleAdditional() {
		CreateClient.ZAPPER_RENDER_HANDLER.addBeam(new LaserBeam(location, target));
	}

}
