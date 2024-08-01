package com.simibubi.create.content.fluids.transfer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllPackets;
import com.simibubi.create.AllTags.AllFluidTags;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.mixin.fabric.SortedArraySetAccessor;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.NbtFixer;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public abstract class FluidManipulationBehaviour extends BlockEntityBehaviour {

	public static record BlockPosEntry(BlockPos pos, int distance) {
	};

	public static class ChunkNotLoadedException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	BoundingBox affectedArea;
	BlockPos rootPos;
	boolean infinite;
	protected boolean counterpartActed;

	// Search
	static final int searchedPerTick = 1024;
	static final int validationTimerMin = 160;
	List<BlockPosEntry> frontier;
	Set<BlockPos> visited;

	int revalidateIn;

	public FluidManipulationBehaviour(SmartBlockEntity be) {
		super(be);
		setValidationTimer();
		infinite = false;
		visited = new HashSet<>();
		frontier = new ArrayList<>();
	}

	public boolean isInfinite() {
		return infinite;
	}

	public void counterpartActed(TransactionContext ctx) {
		snapshotParticipant().updateSnapshots(ctx);
		counterpartActed = true;
	}

	protected abstract SnapshotParticipant<?> snapshotParticipant();

	protected int validationTimer() {
		int maxBlocks = maxBlocks();
		// Allow enough time for the server's infinite block threshold to be reached
		return maxBlocks < 0 ? validationTimerMin : Math.max(validationTimerMin, maxBlocks / searchedPerTick + 1);
	}

	protected int setValidationTimer() {
		return revalidateIn = validationTimer();
	}

	protected int setLongValidationTimer() {
		return revalidateIn = validationTimer() * 2;
	}

	protected int maxRange() {
		return AllConfigs.server().fluids.hosePulleyRange.get();
	}

	protected int maxBlocks() {
		return AllConfigs.server().fluids.hosePulleyBlockThreshold.get();
	}

	protected boolean fillInfinite() {
		return AllConfigs.server().fluids.fillInfinite.get();
	}

	public void reset(@Nullable TransactionContext ctx) {
		if (affectedArea != null)
			scheduleUpdatesInAffectedArea();
		affectedArea = null;
		setValidationTimer();
		frontier.clear();
		visited.clear();
		infinite = false;
	}

	@Override
	public void destroy() {
		reset(null);
		super.destroy();
	}

	protected void scheduleUpdatesInAffectedArea() {
		Level world = getWorld();
		BlockPos
			.betweenClosedStream(
				new BlockPos(affectedArea.minX() - 1, affectedArea.minY() - 1, affectedArea.minZ() - 1),
				new BlockPos(affectedArea.maxX() + 1, affectedArea.maxY() + 1, affectedArea.maxZ() + 1))
			.forEach(pos -> {
				FluidState nextFluidState = world.getFluidState(pos);
				if (nextFluidState.isEmpty())
					return;
				world.scheduleTick(pos, nextFluidState.getType(), world.getRandom()
					.nextInt(5));
			});
	}

	protected int comparePositions(BlockPosEntry e1, BlockPosEntry e2) {
		Vec3 centerOfRoot = VecHelper.getCenterOf(rootPos);
		BlockPos pos2 = e2.pos;
		BlockPos pos1 = e1.pos;
		if (pos1.getY() != pos2.getY())
			return Integer.compare(pos2.getY(), pos1.getY());
		int compareDistance = Integer.compare(e2.distance, e1.distance);
		if (compareDistance != 0)
			return compareDistance;
		int distanceCompared = Double.compare(VecHelper.getCenterOf(pos2)
						.distanceToSqr(centerOfRoot),
				VecHelper.getCenterOf(pos1)
						.distanceToSqr(centerOfRoot));
		// fabric: since we're using a set for the queue, we need to only have them equal if they're really equal.
		if (distanceCompared != 0)
			return distanceCompared;
		// equidistant, go by X and Z
		int xCompared = Integer.compare(pos2.getX(), pos1.getX());
		if (xCompared != 0)
			return xCompared;
		return Integer.compare(pos2.getZ(), pos1.getZ());
	}

	protected Fluid search(Fluid fluid, List<BlockPosEntry> frontier, Set<BlockPos> visited,
		BiConsumer<BlockPos, Integer> add, boolean searchDownward) throws ChunkNotLoadedException {
		Level world = getWorld();
		int maxBlocks = maxBlocks();
		int maxRange = canDrainInfinitely(fluid) ? maxRange() : maxRange() / 2;
		int maxRangeSq = maxRange * maxRange;
		int i;

		for (i = 0; i < searchedPerTick && !frontier.isEmpty()
			&& (visited.size() <= maxBlocks || !canDrainInfinitely(fluid)); i++) {
			BlockPosEntry entry = frontier.remove(0);
			BlockPos currentPos = entry.pos;
			if (visited.contains(currentPos))
				continue;
			visited.add(currentPos);

			if (!world.isLoaded(currentPos))
				throw new ChunkNotLoadedException();

			FluidState fluidState = world.getFluidState(currentPos);
			if (fluidState.isEmpty())
				continue;

			Fluid currentFluid = FluidHelper.convertToStill(fluidState.getType());
			if (fluid == null)
				fluid = currentFluid;
			if (!currentFluid.isSame(fluid))
				continue;

			add.accept(currentPos, entry.distance);

			for (Direction side : Iterate.directions) {
				if (!searchDownward && side == Direction.DOWN)
					continue;

				BlockPos offsetPos = currentPos.relative(side);
				if (!world.isLoaded(offsetPos))
					throw new ChunkNotLoadedException();
				if (visited.contains(offsetPos))
					continue;
				if (offsetPos.distSqr(rootPos) > maxRangeSq)
					continue;

				FluidState nextFluidState = world.getFluidState(offsetPos);
				if (nextFluidState.isEmpty())
					continue;
				Fluid nextFluid = nextFluidState.getType();
				if (nextFluid == FluidHelper.convertToFlowing(nextFluid) && side == Direction.UP
					&& !VecHelper.onSameAxis(rootPos, offsetPos, Axis.Y))
					continue;

				frontier.add(new BlockPosEntry(offsetPos, entry.distance + 1));
			}
		}

		return fluid;
	}

	protected void playEffect(Level world, BlockPos pos, Fluid fluid, boolean fillSound) {
		if (fluid == null)
			return;

		BlockPos splooshPos = pos == null ? blockEntity.getBlockPos() : pos;
		FluidStack stack = new FluidStack(fluid, 1);

		FluidVariant variant = FluidVariant.of(fluid);
		SoundEvent soundevent = fillSound
				? FluidVariantAttributes.getFillSound(variant)
				: FluidVariantAttributes.getEmptySound(variant);

		world.playSound(null, splooshPos, soundevent, SoundSource.BLOCKS, 0.3F, 1.0F);
		if (world instanceof ServerLevel)
			AllPackets.sendToNear(world, splooshPos, 10, new FluidSplashPacket(splooshPos, stack));
	}

	protected boolean canDrainInfinitely(Fluid fluid) {
		if (fluid == null)
			return false;
		return maxBlocks() != -1 && AllConfigs.server().fluids.bottomlessFluidMode.get()
			.test(fluid);
	}

	@Override
	public void write(CompoundTag nbt, boolean clientPacket) {
		if (infinite)
			NBTHelper.putMarker(nbt, "Infinite");
		if (rootPos != null)
			nbt.put("LastPos", NbtUtils.writeBlockPos(rootPos));
		if (affectedArea != null) {
			nbt.put("AffectedAreaFrom",
				NbtUtils.writeBlockPos(new BlockPos(affectedArea.minX(), affectedArea.minY(), affectedArea.minZ())));
			nbt.put("AffectedAreaTo",
				NbtUtils.writeBlockPos(new BlockPos(affectedArea.maxX(), affectedArea.maxY(), affectedArea.maxZ())));
		}
		super.write(nbt, clientPacket);
	}

	@Override
	public void read(CompoundTag nbt, boolean clientPacket) {
		infinite = nbt.contains("Infinite");
		if (nbt.contains("LastPos"))
			rootPos = NbtFixer.readBlockPos(nbt, "LastPos");
		if (nbt.contains("AffectedAreaFrom") && nbt.contains("AffectedAreaTo"))
			affectedArea = BoundingBox.fromCorners(NbtFixer.readBlockPos(nbt, "AffectedAreaFrom"),
					NbtFixer.readBlockPos(nbt, "AffectedAreaTo"));
		super.read(nbt, clientPacket);
	}

	public enum BottomlessFluidMode implements Predicate<Fluid> {
		ALLOW_ALL(fluid -> true),
		DENY_ALL(fluid -> false),
		ALLOW_BY_TAG(fluid -> AllFluidTags.BOTTOMLESS_ALLOW.matches(fluid)),
		DENY_BY_TAG(fluid -> !AllFluidTags.BOTTOMLESS_DENY.matches(fluid));

		private final Predicate<Fluid> predicate;

		BottomlessFluidMode(Predicate<Fluid> predicate) {
			this.predicate = predicate;
		}

		@Override
		public boolean test(Fluid fluid) {
			return predicate.test(fluid);
		}
	}


	/**
	 * Quickly copy the given set.
	 * This is a shallow copy, so entries must be immutable.
	 */
	public static <T> SortedArraySet<T> copySet(SortedArraySet<T> set) {
		int size = set.size();
		SortedArraySetAccessor<T> access = (SortedArraySetAccessor<T>) set;
		Comparator<T> comparator = access.create$getComparator();
		T[] contents = access.create$getContents();
		T[] copiedContents = (T[]) new Object[size];
		System.arraycopy(contents, 0, copiedContents, 0, size);
		SortedArraySet<T> copy = SortedArraySet.create(comparator, size);
		SortedArraySetAccessor<T> copyAccess = ((SortedArraySetAccessor<T>) copy);
		copyAccess.create$setContents(copiedContents);
		copyAccess.create$setSize(size);
		return copy;
	}

	/**
	 * Remove the first entry from the given set.
	 * identical to {@code set.remove(set.first())}
	 */
	public static <T> void dequeue(SortedArraySet<T> set) {
		((SortedArraySetAccessor<T>) set).create$callRemoveInternal(0);
	}

}
