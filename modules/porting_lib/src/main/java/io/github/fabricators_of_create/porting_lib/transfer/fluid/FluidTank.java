package io.github.fabricators_of_create.porting_lib.transfer.fluid;

import io.github.fabricators_of_create.porting_lib.PortingLibBase;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;

import java.util.function.Predicate;

import com.mojang.logging.LogUtils;

public class FluidTank extends SingleVariantStorage<FluidVariant> {
	protected Predicate<FluidStack> validator;
	protected long capacity;
	protected FluidStack stack = FluidStack.EMPTY;

	public FluidTank(FluidStack fluid, long capacity) {
		this(capacity);
		setFluid(fluid);
	}

	public FluidTank(long capacity) {
		this(capacity, fluidStack -> true);
	}

	public FluidTank(long capacity, Predicate<FluidStack> validator) {
		this.capacity = capacity;
		this.validator = validator;
	}

	public FluidTank setValidator(Predicate<FluidStack> validator) {
		if (validator != null) {
			this.validator = validator;
		}
		return this;
	}

	public boolean isFluidValid(FluidStack stack) {
		if(stack.getFluid() == Fluids.EMPTY && stack.getAmount() != 0) {
			LogUtils.getLogger().warn("FluidStack state is corrupted!!!"); 
			return false;
		}
		return validator.test(stack);
	}

	@Override
	public long insert(FluidVariant insertedVariant, long maxAmount, TransactionContext transaction) {
		if (!isFluidValid(new FluidStack(insertedVariant, maxAmount)))
			return 0;
		long inserted = super.insert(insertedVariant, maxAmount, transaction);
		updateStack();
		return inserted;
	}

	@Override
	public long extract(FluidVariant extractedVariant, long maxAmount, TransactionContext transaction) {
		if (!isFluidValid(new FluidStack(extractedVariant, maxAmount)))
			return 0;
		long extracted = super.extract(extractedVariant, maxAmount, transaction);
		updateStack();
		return extracted;
	}

	private void updateStack() {
		this.stack = new FluidStack(this.variant, this.amount);
	}

	public FluidTank setCapacity(long capacity) {
		this.capacity = capacity;
		return this;
	}

	@Override
	protected FluidVariant getBlankVariant() {
		return FluidStack.EMPTY.getType();
	}

	@Override
	protected long getCapacity(FluidVariant variant) {
		return getCapacity();
	}

	public long getCapacity() {
		return capacity;
	}

	public FluidStack getFluid() {
		return stack;
	}

	public void setFluid(FluidStack fluid) {
		if(fluid == null) fluid = FluidStack.EMPTY;
		this.variant = fluid.getType();
		this.amount = fluid.getAmount();
		this.stack = fluid;
	}

	public CompoundTag writeToNBT(CompoundTag tag) {
		updateStack();
		tag = (CompoundTag)stack.saveOptional(PortingLibBase.getRegistryAccess(), tag);
		tag.putLong("Capacity", capacity);
		//LogUtils.getLogger().info("write: "+capacity+", "+stack+", "+tag);
		return tag;
	}

	public FluidTank readFromNBT(CompoundTag tag) {
		//LogUtils.getLogger().info("read: "+tag);
		setFluid(FluidStack.parse(PortingLibBase.getRegistryAccess(), tag).orElse(FluidStack.EMPTY));
		if (tag.contains("Capacity")) this.capacity = tag.getLong("Capacity");
		return this;
	}

	public boolean isEmpty() {
		return getFluid() == null || getFluid().isEmpty();
	}

	public long getFluidAmount() {
		return amount;
	}

	public long getSpace() {
		return Math.max(0, capacity - getFluid().getAmount());
	}

	@Override
	protected void onFinalCommit() {
		updateStack();
		onContentsChanged();
	}

	@Override
	protected void readSnapshot(ResourceAmount<FluidVariant> snapshot) {
		super.readSnapshot(snapshot);
		updateStack();
	}

	protected void onContentsChanged() {
	}
}
