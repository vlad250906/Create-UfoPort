package com.simibubi.create.content.contraptions.glue;

import java.util.Set;

import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.fabric.ReachUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SuperGlueSelectionPacket extends SimplePacketBase {

	private BlockPos from;
	private BlockPos to;

	public SuperGlueSelectionPacket(BlockPos from, BlockPos to) {
		this.from = from;
		this.to = to;
	}

	public SuperGlueSelectionPacket(RegistryFriendlyByteBuf buffer) {
		from = buffer.readBlockPos();
		to = buffer.readBlockPos();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(from);
		buffer.writeBlockPos(to);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (AdventureUtil.isAdventure(player))
				return;

			double range = ReachUtil.reach(player) + 2;
			if (player.distanceToSqr(Vec3.atCenterOf(to)) > range * range)
				return;
			if (!to.closerThan(from, 25))
				return;

			Set<BlockPos> group = SuperGlueSelectionHelper.searchGlueGroup(player.level(), from, to, false);
			if (group == null)
				return;
			if (!group.contains(to))
				return;
			if (!SuperGlueSelectionHelper.collectGlueFromInventory(player, 1, true))
				return;

			AABB bb = SuperGlueEntity.span(from, to);
			SuperGlueSelectionHelper.collectGlueFromInventory(player, 1, false);
			SuperGlueEntity entity = new SuperGlueEntity(player.level(), bb);
			player.level().addFreshEntity(entity);
			entity.spawnParticles();

			AllAdvancements.SUPER_GLUE.awardTo(player);
		});
		return true;
	}

}
