package com.simibubi.create.content.contraptions.sync;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.fabric.ReachUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public class ContraptionInteractionPacket extends SimplePacketBase {

	private InteractionHand interactionHand;
	private int target;
	private BlockPos localPos;
	private Direction face;

	public ContraptionInteractionPacket(AbstractContraptionEntity target, InteractionHand hand, BlockPos localPos, Direction side) {
		this.interactionHand = hand;
		this.localPos = localPos;
		this.target = target.getId();
		this.face = side;
	}

	public ContraptionInteractionPacket(RegistryFriendlyByteBuf buffer) {
		target = buffer.readInt();
		int handId = buffer.readInt();
		interactionHand = handId == -1 ? null : InteractionHand.values()[handId];
		localPos = buffer.readBlockPos();
		face = Direction.from3DDataValue(buffer.readShort());
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeInt(target);
		buffer.writeInt(interactionHand == null ? -1 : interactionHand.ordinal());
		buffer.writeBlockPos(localPos);
		buffer.writeShort(face.get3DDataValue());
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer sender = context.getSender();
			if (sender == null)
				return;
			Entity entityByID = sender.level().getEntity(target);
			if (!(entityByID instanceof AbstractContraptionEntity))
				return;
			AbstractContraptionEntity contraptionEntity = (AbstractContraptionEntity) entityByID;
			AABB bb = contraptionEntity.getBoundingBox();
			double boundsExtra = Math.max(bb.getXsize(), bb.getYsize());
			double d = ReachUtil.reach(sender) + 10 + boundsExtra;
			if (!sender.hasLineOfSight(entityByID))
				d -= 3;
			d *= d;
			if (sender.distanceToSqr(entityByID) > d)
				return;
			if (contraptionEntity.handlePlayerInteraction(sender, localPos, face, interactionHand))
				sender.swing(interactionHand, true);
		});
		return true;
	}

}
