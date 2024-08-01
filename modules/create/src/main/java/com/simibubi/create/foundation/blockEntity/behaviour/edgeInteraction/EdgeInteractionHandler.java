package com.simibubi.create.foundation.blockEntity.behaviour.edgeInteraction;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.RaycastHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class EdgeInteractionHandler {

	public static InteractionResult onBlockActivated(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
//		Level world = event.getWorld();
		BlockPos pos = hitResult.getBlockPos();//event.getPos();
//		Player player = event.getPlayer();
//		InteractionHand hand = event.getHand();
		ItemStack heldItem = player.getItemInHand(hand);

		if (player.isShiftKeyDown() || player.isSpectator() || AdventureUtil.isAdventure(player))
			return InteractionResult.PASS;
		EdgeInteractionBehaviour behaviour = BlockEntityBehaviour.get(world, pos, EdgeInteractionBehaviour.TYPE);
		if (behaviour == null)
			return InteractionResult.PASS;
		if (!behaviour.requiredPredicate.test(heldItem.getItem()))
			return InteractionResult.PASS;
		BlockHitResult ray = RaycastHelper.rayTraceRange(world, player, 10);
		if (ray == null)
			return InteractionResult.PASS;

		Direction activatedDirection = getActivatedDirection(world, pos, ray.getDirection(), ray.getLocation(), behaviour);
		if (activatedDirection == null)
			return InteractionResult.PASS;

		if (!world.isClientSide())
			behaviour.connectionCallback.apply(world, pos, pos.relative(activatedDirection));
//		event.setCanceled(true);
//		event.setCancellationResult(InteractionResult.SUCCESS);
		world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .25f, .1f);
		return InteractionResult.SUCCESS;
	}

	public static List<Direction> getConnectiveSides(Level world, BlockPos pos, Direction face,
		EdgeInteractionBehaviour behaviour) {
		List<Direction> sides = new ArrayList<>(6);
		if (BlockHelper.hasBlockSolidSide(world.getBlockState(pos.relative(face)), world, pos.relative(face), face.getOpposite()))
			return sides;

		for (Direction direction : Iterate.directions) {
			if (direction.getAxis() == face.getAxis())
				continue;
			BlockPos neighbourPos = pos.relative(direction);
			if (BlockHelper.hasBlockSolidSide(world.getBlockState(neighbourPos.relative(face)), world, neighbourPos.relative(face),
				face.getOpposite()))
				continue;
			if (!behaviour.connectivityPredicate.test(world, pos, face, direction))
				continue;
			sides.add(direction);
		}

		return sides;
	}

	public static Direction getActivatedDirection(Level world, BlockPos pos, Direction face, Vec3 hit,
		EdgeInteractionBehaviour behaviour) {
		for (Direction facing : getConnectiveSides(world, pos, face, behaviour)) {
			AABB bb = getBB(pos, facing);
			if (bb.contains(hit))
				return facing;
		}
		return null;
	}

	static AABB getBB(BlockPos pos, Direction direction) {
		AABB bb = new AABB(pos);
		Vec3i vec = direction.getNormal();
		int x = vec.getX();
		int y = vec.getY();
		int z = vec.getZ();
		double margin = 10 / 16f;
		double absX = Math.abs(x) * margin;
		double absY = Math.abs(y) * margin;
		double absZ = Math.abs(z) * margin;

		bb = bb.contract(absX, absY, absZ);
		bb = bb.move(absX / 2d, absY / 2d, absZ / 2d);
		bb = bb.move(x / 2d, y / 2d, z / 2d);
		bb = bb.inflate(1 / 256f);
		return bb;
	}

}
