package com.simibubi.create.content.fluids;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.actors.psi.PortableFluidInterfaceBlockEntity.InterfaceFluidHandler;
import com.simibubi.create.content.fluids.PipeConnection.Flow;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Pair;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class FluidNetwork {

	private static int CYCLES_PER_TICK = 16;

	Level world;
	BlockFace start;

	Supplier<Storage<FluidVariant>> sourceSupplier;
	Storage<FluidVariant> source;
	int transferSpeed;

	int pauseBeforePropagation;
	List<BlockFace> queued;
	Set<Pair<BlockFace, PipeConnection>> frontier;
	Set<BlockPos> visited;
	FluidStack fluid;
	List<Pair<BlockFace, Storage<FluidVariant>>> targets;
	Map<BlockPos, WeakReference<FluidTransportBehaviour>> cache;

	public FluidNetwork(Level world, BlockFace location, Supplier<Storage<FluidVariant>> sourceSupplier) {
		this.world = world;
		this.start = location;
		this.sourceSupplier = sourceSupplier;
		this.source = null;
		this.fluid = FluidStack.EMPTY;
		this.frontier = new HashSet<>();
		this.visited = new HashSet<>();
		this.targets = new ArrayList<>();
		this.cache = new HashMap<>();
		this.queued = new ArrayList<>();
		reset();
	}

	public void tick() {
		if (pauseBeforePropagation > 0) {
			pauseBeforePropagation--;
			return;
		}

		for (int cycle = 0; cycle < CYCLES_PER_TICK; cycle++) {
			boolean shouldContinue = false;
			for (Iterator<BlockFace> iterator = queued.iterator(); iterator.hasNext();) {
				BlockFace blockFace = iterator.next();
				if (!isPresent(blockFace))
					continue;
				PipeConnection pipeConnection = get(blockFace);
				if (pipeConnection != null) {
					if (blockFace.equals(start))
						transferSpeed = (int) Math.max(1, pipeConnection.pressure.get(true) / 2f) * 81; // fabric: 81 times speed to match 81 times larger amounts
					frontier.add(Pair.of(blockFace, pipeConnection));
				}
				iterator.remove();
			}

//			drawDebugOutlines();

			for (Iterator<Pair<BlockFace, PipeConnection>> iterator = frontier.iterator(); iterator.hasNext();) {
				Pair<BlockFace, PipeConnection> pair = iterator.next();
				BlockFace blockFace = pair.getFirst();
				PipeConnection pipeConnection = pair.getSecond();

				if (!pipeConnection.hasFlow())
					continue;

				Flow flow = pipeConnection.flow.get();
				if (!fluid.isEmpty() && !flow.fluid.isFluidEqual(fluid)) {
					iterator.remove();
					continue;
				}
				if (!flow.inbound) {
					if (pipeConnection.comparePressure() >= 0)
						iterator.remove();
					continue;
				}
				if (!flow.complete)
					continue;

				if (fluid.isEmpty())
					fluid = flow.fluid;

				boolean canRemove = true;
				for (Direction side : Iterate.directions) {
					if (side == blockFace.getFace())
						continue;
					BlockFace adjacentLocation = new BlockFace(blockFace.getPos(), side);
					PipeConnection adjacent = get(adjacentLocation);
					if (adjacent == null)
						continue;
					if (!adjacent.hasFlow()) {
						// Branch could potentially still appear
						if (adjacent.hasPressure() && adjacent.pressure.getSecond() > 0)
							canRemove = false;
						continue;
					}
					Flow outFlow = adjacent.flow.get();
					if (outFlow.inbound) {
						if (adjacent.comparePressure() > 0)
							canRemove = false;
						continue;
					}
					if (!outFlow.complete) {
						canRemove = false;
						continue;
					}

					// Give pipe end a chance to init connections
					if (!adjacent.source.isPresent() && !adjacent.determineSource(world, blockFace.getPos())) {
						canRemove = false;
						continue;
					}

					if (adjacent.source.isPresent() && adjacent.source.get()
						.isEndpoint()) {
						targets.add(Pair.of(adjacentLocation, adjacent.source.get()
							.provideHandler()));
						continue;
					}

					if (visited.add(adjacentLocation.getConnectedPos())) {
						queued.add(adjacentLocation.getOpposite());
						shouldContinue = true;
					}
				}
				if (canRemove)
					iterator.remove();
			}
			if (!shouldContinue)
				break;
		}

//		drawDebugOutlines();

		if (source == null)
			source = sourceSupplier.get();
		if (source == null)
			return;

		keepPortableFluidInterfaceEngaged();

		if (targets.isEmpty())
			return;
		for (Pair<BlockFace, Storage<FluidVariant>> pair : targets) {
			if (pair.getSecond()
				!= null && world.getGameTime() % 40 != 0)
				continue;
			PipeConnection pipeConnection = get(pair.getFirst());
			if (pipeConnection == null)
				continue;
			pipeConnection.source.ifPresent(fs -> {
				if (fs.isEndpoint())
					pair.setSecond(fs.provideHandler());
			});
		}

		long flowSpeed = transferSpeed;
		try (Transaction t = TransferUtil.getTransaction()) {

			Storage<FluidVariant> handler = source;
			if (handler == null)
				return;

			FluidStack transfer = FluidStack.EMPTY;
			long transferredAmount = 0;
			try (Transaction test = t.openNested()) {
				long extracted = handler.extract(fluid.getType(), flowSpeed, test);
				if (extracted > 0) transfer = new FluidStack(fluid.getType(), extracted);

				if (transfer.isEmpty())
					return;
				test.abort();
			}
			List<Pair<BlockFace, Storage<FluidVariant>>> availableOutputs = new ArrayList<>(targets);

			while (!availableOutputs.isEmpty() && transfer.getAmount() > 0) {
				long dividedTransfer = transfer.getAmount() / availableOutputs.size();
				long remainder = transfer.getAmount() % availableOutputs.size();

				for (Iterator<Pair<BlockFace, Storage<FluidVariant>>> iterator =
					 availableOutputs.iterator(); iterator.hasNext();) {
					Pair<BlockFace, Storage<FluidVariant>> pair = iterator.next();
					long toTransfer = dividedTransfer;
					if (remainder > 0) {
						toTransfer++;
						remainder--;
					}

					if (transfer.isEmpty())
						break;
					Storage<FluidVariant> targetHandler = pair.getSecond();
					if (targetHandler == null) {
						iterator.remove();
						continue;
					}

					FluidStack divided = transfer.copy();
					divided.setAmount(toTransfer);
					long fill = targetHandler.insert(divided.getType(), divided.getAmount(), t);

					transfer.setAmount(transfer.getAmount() - fill);
					transferredAmount += fill;
					if (fill < toTransfer)
						iterator.remove();
				}

			}

			try (Transaction extract = t.openNested()) {
				handler.extract(fluid.getType(), transferredAmount, extract);
				extract.commit();
			}
			t.commit();
		}
	}

//	private void drawDebugOutlines() {
//		FluidPropagator.showBlockFace(start)
//			.lineWidth(1 / 8f)
//			.colored(0xff0000);
//		for (Pair<BlockFace, LazyOptional<IFluidHandler>> pair : targets)
//			FluidPropagator.showBlockFace(pair.getFirst())
//				.lineWidth(1 / 8f)
//				.colored(0x00ff00);
//		for (Pair<BlockFace, PipeConnection> pair : frontier)
//			FluidPropagator.showBlockFace(pair.getFirst())
//				.lineWidth(1 / 4f)
//				.colored(0xfaaa33);
//	}

	private void keepPortableFluidInterfaceEngaged() {
		Storage<FluidVariant> handler = source;
		if (!(handler instanceof InterfaceFluidHandler))
			return;
		if (frontier.isEmpty())
			return;
		((InterfaceFluidHandler) handler).keepAlive();
	}

	public void reset() {
		frontier.clear();
		visited.clear();
		targets.clear();
		queued.clear();
		fluid = FluidStack.EMPTY;
		queued.add(start);
		pauseBeforePropagation = 2;
	}

	@Nullable
	private PipeConnection get(BlockFace location) {
		BlockPos pos = location.getPos();
		FluidTransportBehaviour fluidTransfer = getFluidTransfer(pos);
		if (fluidTransfer == null)
			return null;
		return fluidTransfer.getConnection(location.getFace());
	}

	private boolean isPresent(BlockFace location) {
		return world.isLoaded(location.getPos());
	}

	@Nullable
	private FluidTransportBehaviour getFluidTransfer(BlockPos pos) {
		WeakReference<FluidTransportBehaviour> weakReference = cache.get(pos);
		FluidTransportBehaviour behaviour = weakReference != null ? weakReference.get() : null;
		if (behaviour != null && behaviour.blockEntity.isRemoved())
			behaviour = null;
		if (behaviour == null) {
			behaviour = BlockEntityBehaviour.get(world, pos, FluidTransportBehaviour.TYPE);
			if (behaviour != null)
				cache.put(pos, new WeakReference<>(behaviour));
		}
		return behaviour;
	}

}
