package com.simibubi.create.foundation.blockEntity.behaviour.inventory;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Predicates;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.util.StorageProvider;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class InvManipulationBehaviour extends CapManipulationBehaviourBase<ItemVariant, InvManipulationBehaviour> {

	// Extra types available for multibehaviour
	public static final BehaviourType<InvManipulationBehaviour>

	TYPE = new BehaviourType<>(), EXTRACT = new BehaviourType<>(), INSERT = new BehaviourType<>();

	private BehaviourType<InvManipulationBehaviour> behaviourType;

	public static InvManipulationBehaviour forExtraction(SmartBlockEntity be, InterfaceProvider target) {
		return new InvManipulationBehaviour(EXTRACT, be, target);
	}

	public static InvManipulationBehaviour forInsertion(SmartBlockEntity be, InterfaceProvider target) {
		return new InvManipulationBehaviour(INSERT, be, target);
	}

	public InvManipulationBehaviour(SmartBlockEntity be, InterfaceProvider target) {
		this(TYPE, be, target);
	}

	private InvManipulationBehaviour(BehaviourType<InvManipulationBehaviour> type, SmartBlockEntity be,
		InterfaceProvider target) {
		super(be, target);
		behaviourType = type;
	}

	@Override
	protected StorageProvider<ItemVariant> getProvider(BlockPos pos, boolean bypassSided) {
		return bypassSided
				? new UnsidedItemStorageProvider(getWorld(), pos)
				: StorageProvider.createForItems(getWorld(), pos);
	}

	public ItemStack extract() {
		return extract(getModeFromFilter(), getAmountFromFilter());
	}

	public ItemStack extract(ExtractionCountMode mode, int amount) {
		return extract(mode, amount, Predicates.alwaysTrue());
	}

	public ItemStack extract(ExtractionCountMode mode, int amount, Predicate<ItemStack> filter) {
		boolean shouldSimulate = simulateNext;
		simulateNext = false;

		if (getWorld().isClientSide || !hasInventory())
			return ItemStack.EMPTY;
		Storage<ItemVariant> inventory = getInventory();
		if (inventory == null)
			return ItemStack.EMPTY;

		Predicate<ItemStack> test = getFilterTest(filter);
		ItemStack simulatedItems = ItemHelper.extract(inventory, test, mode, amount, true);
		if (shouldSimulate || simulatedItems.isEmpty())
			return simulatedItems;
		return ItemHelper.extract(inventory, test, mode, amount, false);
	}

	public ItemStack insert(ItemStack stack) {
		boolean shouldSimulate = simulateNext;
		simulateNext = false;
		Storage<ItemVariant> inventory = hasInventory() ? getInventory() : null;
		if (inventory == null)
			return stack;
		try (Transaction t = TransferUtil.getTransaction()) {
			long inserted = inventory.insert(ItemVariant.of(stack), stack.getCount(), t);
			if (!shouldSimulate) t.commit();
			long remainder = stack.getCount() - inserted;
			if (remainder == 0)
				return ItemStack.EMPTY;
			stack = stack.copy();
			stack.setCount((int) remainder);
			return stack;
		}
	}

	protected Predicate<ItemStack> getFilterTest(Predicate<ItemStack> customFilter) {
		Predicate<ItemStack> test = customFilter;
		FilteringBehaviour filter = blockEntity.getBehaviour(FilteringBehaviour.TYPE);
		if (filter != null)
			test = customFilter.and(filter::test);
		return test;
	}

	@Override
	public BehaviourType<?> getType() {
		return behaviourType;
	}

	public static class UnsidedItemStorageProvider extends UnsidedStorageProvider<ItemVariant> {
		protected UnsidedItemStorageProvider(Level level, BlockPos pos) {
			super(ItemStorage.SIDED, level, pos);
		}

		@Nullable
		@Override
		public Storage<ItemVariant> get() {
			return TransferUtil.getItemStorage(level, pos);
		}
	}
}
