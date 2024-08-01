package com.simibubi.create.content.equipment.toolbox;

import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.ISyncPersistentData.PersistentDataPacket;
import com.simibubi.create.foundation.utility.NbtFixer;
import com.simibubi.create.foundation.utility.WorldAttached;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class ToolboxHandler {

	public static final WorldAttached<WeakHashMap<BlockPos, ToolboxBlockEntity>> toolboxes =
		new WorldAttached<>(w -> new WeakHashMap<>());

	public static void onLoad(ToolboxBlockEntity be) {
		toolboxes.get(be.getLevel())
			.put(be.getBlockPos(), be);
	}

	public static void onUnload(ToolboxBlockEntity be) {
		toolboxes.get(be.getLevel())
			.remove(be.getBlockPos());
	}

	static int validationTimer = 20;

	public static void entityTick(Entity entity, Level world) {
		if (world.isClientSide)
			return;
		if (!(world instanceof ServerLevel))
			return;
		if (!(entity instanceof ServerPlayer))
			return;
		if (entity.tickCount % validationTimer != 0)
			return;

		ServerPlayer player = (ServerPlayer) entity;
		if (!player.getCustomData()
			.contains("CreateToolboxData"))
			return;

		boolean sendData = false;
		CompoundTag compound = player.getCustomData()
			.getCompound("CreateToolboxData");
		for (int i = 0; i < 9; i++) {
			String key = String.valueOf(i);
			if (!compound.contains(key))
				continue;

			CompoundTag data = compound.getCompound(key);
			BlockPos pos = NbtFixer.readBlockPos(data, "Pos");
			int slot = data.getInt("Slot");

			if (!world.isLoaded(pos))
				continue;
			if (!(world.getBlockState(pos)
				.getBlock() instanceof ToolboxBlock)) {
				compound.remove(key);
				sendData = true;
				continue;
			}

			BlockEntity prevBlockEntity = world.getBlockEntity(pos);
			if (prevBlockEntity instanceof ToolboxBlockEntity)
				((ToolboxBlockEntity) prevBlockEntity).connectPlayer(slot, player, i);
		}

		if (sendData)
			syncData(player);
	}

	public static void playerLogin(Player player) {
		if (!(player instanceof ServerPlayer))
			return;
		if (player.getCustomData()
			.contains("CreateToolboxData")
			&& !player.getCustomData()
				.getCompound("CreateToolboxData")
				.isEmpty()) {
			syncData(player);
		}
	}

	public static void syncData(Player player) {
		AllPackets.getChannel().sendToClient(new PersistentDataPacket(player), (ServerPlayer) player);
	}

	public static List<ToolboxBlockEntity> getNearest(LevelAccessor world, Player player, int maxAmount) {
		Vec3 location = player.position();
		double maxRange = getMaxRange(player);
		return toolboxes.get(world)
			.keySet()
			.stream()
			.filter(p -> distance(location, p) < maxRange * maxRange)
			.sorted((p1, p2) -> Double.compare(distance(location, p1), distance(location, p2)))
			.limit(maxAmount)
			.map(toolboxes.get(world)::get)
			.filter(ToolboxBlockEntity::isFullyInitialized)
			.collect(Collectors.toList());
	}

	public static void unequip(Player player, int hotbarSlot, boolean keepItems) {
		CompoundTag compound = player.getCustomData()
			.getCompound("CreateToolboxData");
		Level world = player.level();
		String key = String.valueOf(hotbarSlot);
		if (!compound.contains(key))
			return;

		CompoundTag prevData = compound.getCompound(key);
		BlockPos prevPos = NbtFixer.readBlockPos(prevData, "Pos");
		int prevSlot = prevData.getInt("Slot");

		BlockEntity prevBlockEntity = world.getBlockEntity(prevPos);
		if (prevBlockEntity instanceof ToolboxBlockEntity) {
			ToolboxBlockEntity toolbox = (ToolboxBlockEntity) prevBlockEntity;
			toolbox.unequip(prevSlot, player, hotbarSlot, keepItems || !ToolboxHandler.withinRange(player, toolbox));
		}
		compound.remove(key);
	}

	public static boolean withinRange(Player player, ToolboxBlockEntity box) {
		if (player.level() != box.getLevel())
			return false;
		double maxRange = getMaxRange(player);
		return distance(player.position(), box.getBlockPos()) < maxRange * maxRange;
	}

	public static double distance(Vec3 location, BlockPos p) {
		return location.distanceToSqr(p.getX() + 0.5f, p.getY(), p.getZ() + 0.5f);
	}

	public static double getMaxRange(Player player) {
		return AllConfigs.server().equipment.toolboxRange.get()
			.doubleValue();
	}

}
