package com.simibubi.create.content.contraptions.minecart;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.simibubi.create.content.contraptions.Contraption.ContraptionInvWrapper;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.utility.fabric.ListeningStorageView;
import com.simibubi.create.foundation.utility.fabric.ProcessingIterator;

import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TrainCargoManager extends MountedStorageManager {

	int ticksSinceLastExchange;
	AtomicInteger version;

	public TrainCargoManager() {
		version = new AtomicInteger();
		ticksSinceLastExchange = 0;
	}

	@Override
	public void createHandlers() {
		super.createHandlers();
	}

	@Override
	protected ContraptionInvWrapper wrapItems(Collection<? extends Storage<ItemVariant>> list, boolean fuel) {
		if (fuel)
			return super.wrapItems(list, fuel);
		return new CargoInvWrapper(Arrays.copyOf(list.toArray(), list.size(), Storage[].class));
	}

	@Override
	protected CombinedTankWrapper wrapFluids(Collection<? extends Storage<FluidVariant>> list) {
		return new CargoTankWrapper(Arrays.copyOf(list.toArray(), list.size(), Storage[].class));
	}

	@Override
	public void write(CompoundTag nbt, boolean clientPacket) {
		super.write(nbt, clientPacket);
		nbt.putInt("TicksSinceLastExchange", ticksSinceLastExchange);
	}

	@Override
	public void read(CompoundTag nbt, Map<BlockPos, BlockEntity> presentBlockEntities, boolean clientPacket) {
		super.read(nbt, presentBlockEntities, clientPacket);
		ticksSinceLastExchange = nbt.getInt("TicksSinceLastExchange");
	}

	public void resetIdleCargoTracker() {
		ticksSinceLastExchange = 0;
	}

	public void tickIdleCargoTracker() {
		ticksSinceLastExchange++;
	}

	public int getTicksSinceLastExchange() {
		return ticksSinceLastExchange;
	}

	public int getVersion() {
		return version.get();
	}

	void changeDetected() {
		version.incrementAndGet();
		resetIdleCargoTracker();
	}

	class CargoInvWrapper extends ContraptionInvWrapper {

		public CargoInvWrapper(Storage<ItemVariant>... itemHandler) {
			super(false, itemHandler);
		}

		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			long inserted = super.insert(resource, maxAmount, transaction);
			if (inserted != 0)
				TransactionCallback.onSuccess(transaction, TrainCargoManager.this::changeDetected);
			return inserted;
		}

		@Override
		public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			long extracted = super.extract(resource, maxAmount, transaction);
			if (extracted != 0)
				TransactionCallback.onSuccess(transaction, TrainCargoManager.this::changeDetected);
			return extracted;
		}

		@Override
		public Iterator<StorageView<ItemVariant>> iterator() {
			return new ProcessingIterator<>(super.iterator(), view -> new ListeningStorageView<>(view, TrainCargoManager.this::changeDetected));
		}

	}

	class CargoTankWrapper extends CombinedTankWrapper {

		public final SnapshotParticipant<Unit> successListener = new SnapshotParticipant<>() {

			@Override
			protected Unit createSnapshot() {
				return Unit.INSTANCE;
			}

			@Override
			protected void readSnapshot(Unit snapshot) {
			}

			@Override
			protected void onFinalCommit() {
				super.onFinalCommit();
				changeDetected();
			}
		};

		public CargoTankWrapper(Storage<FluidVariant>... fluidHandler) {
			super(fluidHandler);
		}

		@Override
		public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			long inserted = super.insert(resource, maxAmount, transaction);
			if (inserted != 0)
				successListener.updateSnapshots(transaction);
			return inserted;
		}

		@Override
		public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			long extracted = super.extract(resource, maxAmount, transaction);
			if (extracted != 0)
				successListener.updateSnapshots(transaction);
			return extracted;
		}

		@Override
		public Iterator<StorageView<FluidVariant>> iterator() {
//			successListener.updateSnapshots(transaction);
			return super.iterator();
		}
	}

}
