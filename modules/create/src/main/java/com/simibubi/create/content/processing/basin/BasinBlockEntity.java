package com.simibubi.create.content.processing.basin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidFX;
import com.simibubi.create.content.fluids.particle.FluidParticleData;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;
import com.simibubi.create.foundation.utility.LongAttached;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.util.FluidTextUtil;
import io.github.fabricators_of_create.porting_lib.util.FluidUnit;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import io.github.fabricators_of_create.porting_lib.util.StorageProvider;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BasinBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, SidedStorageBlockEntity {

	private boolean needsUpdate; // fabric: need to delay to avoid doing stuff mid-transaction, causing a crash
	private boolean areFluidsMoving;
	LerpedFloat ingredientRotationSpeed;
	LerpedFloat ingredientRotation;

	public BasinInventory inputInventory;
	public SmartFluidTankBehaviour inputTank;
	protected SmartInventory outputInventory;
	protected SmartFluidTankBehaviour outputTank;
	private FilteringBehaviour filtering;
	private boolean contentsChanged;

	private Couple<SmartInventory> invs;
	private Couple<SmartFluidTankBehaviour> tanks;

	protected Storage<ItemVariant> itemCapability;
	protected Storage<FluidVariant> fluidCapability;

	List<Direction> disabledSpoutputs;
	Direction preferredSpoutput;
	protected List<ItemStack> spoutputBuffer;
	protected List<FluidStack> spoutputFluidBuffer;
	int recipeBackupCheck;

	public static final int OUTPUT_ANIMATION_TIME = 10;
	List<LongAttached<ItemStack>> visualizedOutputItems;
	List<LongAttached<FluidStack>> visualizedOutputFluids;

	// fabric: transfer things

	private final Map<Direction, Pair<StorageProvider<ItemVariant>, StorageProvider<FluidVariant>>> spoutputOutputs = new HashMap<>();

	SnapshotParticipant<Data> snapshotParticipant = new SnapshotParticipant<>() {
		@Override
		protected Data createSnapshot() {
			return new Data(new ArrayList<>(spoutputBuffer), new ArrayList<>(spoutputFluidBuffer));
		}

		@Override
		protected void readSnapshot(Data snapshot) {
			spoutputBuffer = snapshot.spoutputBuffer;
			spoutputFluidBuffer = snapshot.spoutputFluidBuffer;
		}
	};

	record Data(List<ItemStack> spoutputBuffer, List<FluidStack> spoutputFluidBuffer) {
	}

	public BasinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inputInventory = new BasinInventory(9, this);
		inputInventory.whenContentsChanged(() -> contentsChanged = true);
		outputInventory = new BasinInventory(9, this).forbidInsertion()
			.withMaxStackSize(64);
		areFluidsMoving = false;
		itemCapability = new CombinedStorage<>(List.of(inputInventory, outputInventory));
		contentsChanged = true;
		ingredientRotation = LerpedFloat.angular()
			.startWithValue(0);
		ingredientRotationSpeed = LerpedFloat.linear()
			.startWithValue(0);

		invs = Couple.create(inputInventory, outputInventory);
		tanks = Couple.create(inputTank, outputTank);
		visualizedOutputItems = Collections.synchronizedList(new ArrayList<>());
		visualizedOutputFluids = Collections.synchronizedList(new ArrayList<>());
		disabledSpoutputs = new ArrayList<>();
		preferredSpoutput = null;
		spoutputBuffer = new ArrayList<>();
		spoutputFluidBuffer = new ArrayList<>();
		recipeBackupCheck = 20;
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		spoutputOutputs.clear();
		for (Direction direction : Iterate.horizontalDirections) {
			BlockPos pos = getBlockPos().below().relative(direction);
			StorageProvider<ItemVariant> items = StorageProvider.createForItems(level, pos);
			StorageProvider<FluidVariant> fluids = StorageProvider.createForFluids(level, pos);
			spoutputOutputs.put(direction, Pair.of(items, fluids));
		}
	}

	public Storage<ItemVariant> getItemSpoutputOutput(Direction facing) {
		Pair<StorageProvider<ItemVariant>, StorageProvider<FluidVariant>> providers = spoutputOutputs.get(facing);
		return providers == null ? null : providers.first().get(facing.getOpposite());
	}

	public Storage<FluidVariant> getFluidSpoutputOutput(Direction facing) {
		Pair<StorageProvider<ItemVariant>, StorageProvider<FluidVariant>> providers = spoutputOutputs.get(facing);
		return providers == null ? null : providers.second().get(facing.getOpposite());
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(this));
		filtering = new FilteringBehaviour(this, new BasinValueBox()).withCallback(newFilter -> contentsChanged = true)
			.forRecipes();
		behaviours.add(filtering);

		inputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 2, FluidConstants.BUCKET, true)
			.whenFluidUpdates(() -> contentsChanged = true);
		outputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 2, FluidConstants.BUCKET, true)
			.whenFluidUpdates(() -> contentsChanged = true)
			.forbidInsertion();
		behaviours.add(inputTank);
		behaviours.add(outputTank);

		fluidCapability = new CombinedTankWrapper(inputTank.getCapability(), outputTank.getCapability());
	}

	@Override
	protected void read(CompoundTag compound, Provider reg, boolean clientPacket) {
		super.read(compound, reg, clientPacket);
		inputInventory.deserializeNBT(compound.getCompound("InputItems"));
		outputInventory.deserializeNBT(compound.getCompound("OutputItems"));

		preferredSpoutput = null;
		if (compound.contains("PreferredSpoutput"))
			preferredSpoutput = NBTHelper.readEnum(compound, "PreferredSpoutput", Direction.class);
		disabledSpoutputs.clear();
		ListTag disabledList = compound.getList("DisabledSpoutput", Tag.TAG_STRING);
		disabledList.forEach(d -> disabledSpoutputs.add(Direction.valueOf(((StringTag) d).getAsString())));
		spoutputBuffer = NBTHelper.readItemList(compound.getList("Overflow", Tag.TAG_COMPOUND));
		spoutputFluidBuffer = NBTHelper.readCompoundList(compound.getList("FluidOverflow", Tag.TAG_COMPOUND),
			obj -> FluidStack.CODEC.decode(RegistryOps.create(NbtOps.INSTANCE, Create.getRegistryAccess()), obj).getOrThrow().getFirst());

		if (!clientPacket)
			return;

		NBTHelper.iterateCompoundList(compound.getList("VisualizedItems", Tag.TAG_COMPOUND),
			c -> visualizedOutputItems.add(LongAttached.with(OUTPUT_ANIMATION_TIME, ItemStack.parseOptional(Create.getRegistryAccess(), c))));
		NBTHelper.iterateCompoundList(compound.getList("VisualizedFluids", Tag.TAG_COMPOUND),
			c -> visualizedOutputFluids
				.add(LongAttached.with(OUTPUT_ANIMATION_TIME, FluidStack.CODEC.decode(RegistryOps.create(NbtOps.INSTANCE, Create.getRegistryAccess()), c).getOrThrow().getFirst())));
	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.put("InputItems", inputInventory.serializeNBT());
		compound.put("OutputItems", outputInventory.serializeNBT());

		if (preferredSpoutput != null)
			NBTHelper.writeEnum(compound, "PreferredSpoutput", preferredSpoutput);
		ListTag disabledList = new ListTag();
		disabledSpoutputs.forEach(d -> disabledList.add(StringTag.valueOf(d.name())));
		compound.put("DisabledSpoutput", disabledList);
		compound.put("Overflow", NBTHelper.writeItemList(spoutputBuffer));
		compound.put("FluidOverflow",
			NBTHelper.writeCompoundList(spoutputFluidBuffer, fs -> (CompoundTag)fs.saveOptional(Create.getRegistryAccess(), new CompoundTag())));

		if (!clientPacket)
			return;

		compound.put("VisualizedItems", NBTHelper.writeCompoundList(visualizedOutputItems, ia -> NBTSerializer.serializeNBTCompound(ia.getValue())));
		compound.put("VisualizedFluids", NBTHelper.writeCompoundList(visualizedOutputFluids, ia -> (CompoundTag)ia.getValue()
			.saveOptional(Create.getRegistryAccess(), new CompoundTag())));
		visualizedOutputItems.clear();
		visualizedOutputFluids.clear();
	}

	@Override
	public void destroy() {
		super.destroy();
		ItemHelper.dropContents(level, worldPosition, inputInventory);
		ItemHelper.dropContents(level, worldPosition, outputInventory);
		spoutputBuffer.forEach(is -> Block.popResource(level, worldPosition, is));
	}

	@Override
	public void remove() {
		super.remove();
		onEmptied();
	}

	public void onEmptied() {
		getOperator().ifPresent(be -> be.basinRemoved = true);
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

//	@Nonnull
//	@Override
//	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, Direction side) {
//		if (cap == ForgeCapabilities.ITEM_HANDLER)
//			return itemCapability.cast();
//		if (cap == ForgeCapabilities.FLUID_HANDLER)
//			return fluidCapability.cast();
//		return super.getCapability(cap, side);
//	}

	@Override
	public void notifyUpdate() {
		this.needsUpdate = true;
	}

	@Override
	public void lazyTick() {
		super.lazyTick();

		if (!level.isClientSide) {
			updateSpoutput();
			if (recipeBackupCheck-- > 0)
				return;
			recipeBackupCheck = 20;
			if (isEmpty())
				return;
			notifyChangeOfContents();
			return;
		}

		BlockEntity blockEntity = level.getBlockEntity(worldPosition.above(2));
		if (!(blockEntity instanceof MechanicalMixerBlockEntity)) {
			setAreFluidsMoving(false);
			return;
		}

		MechanicalMixerBlockEntity mixer = (MechanicalMixerBlockEntity) blockEntity;
		setAreFluidsMoving(mixer.running && mixer.runningTicks <= 20);
	}

	public boolean isEmpty() {
		return inputInventory.isEmpty() && outputInventory.isEmpty() && inputTank.isEmpty() && outputTank.isEmpty();
	}

	public void onWrenched(Direction face) {
		BlockState blockState = getBlockState();
		Direction currentFacing = blockState.getValue(BasinBlock.FACING);

		disabledSpoutputs.remove(face);
		if (currentFacing == face) {
			if (preferredSpoutput == face)
				preferredSpoutput = null;
			disabledSpoutputs.add(face);
		} else
			preferredSpoutput = face;

		updateSpoutput();
	}

	private void updateSpoutput() {
		BlockState blockState = getBlockState();
		Direction currentFacing = blockState.getValue(BasinBlock.FACING);
		Direction newFacing = Direction.DOWN;
		for (Direction test : Iterate.horizontalDirections) {
			boolean canOutputTo = BasinBlock.canOutputTo(level, worldPosition, test);
			if (canOutputTo && !disabledSpoutputs.contains(test))
				newFacing = test;
		}

		if (preferredSpoutput != null && BasinBlock.canOutputTo(level, worldPosition, preferredSpoutput)
			&& preferredSpoutput != Direction.UP)
			newFacing = preferredSpoutput;

		if (newFacing == currentFacing)
			return;

		level.setBlockAndUpdate(worldPosition, blockState.setValue(BasinBlock.FACING, newFacing));

		if (newFacing.getAxis()
			.isVertical())
			return;

		try (Transaction t = TransferUtil.getTransaction()) {
			for (StorageView<ItemVariant> view : outputInventory.nonEmptyViews()) {
				ItemVariant variant = view.getResource();
				ItemStack stack = variant.toStack(ItemHelper.truncateLong(view.getAmount()));
				if (acceptOutputs(ImmutableList.of(stack), ImmutableList.of(), t)) {
					view.extract(variant, stack.getCount(), t);
				}
			}
			for (StorageView<FluidVariant> view : outputTank.getCapability().nonEmptyViews()) {
				FluidVariant variant = view.getResource();
				FluidStack stack = new FluidStack(view);
				if (acceptOutputs(ImmutableList.of(), ImmutableList.of(stack), t)) {
					view.extract(variant, stack.getAmount(), t);
				}
			}
			t.commit();
		}

		notifyChangeOfContents();
		notifyUpdate();
	}

	@Override
	public void tick() {
		super.tick();
		if (needsUpdate) {
			needsUpdate = false;
			super.notifyUpdate();
		}
		if (level.isClientSide) {
			createFluidParticles();
			tickVisualizedOutputs();
			ingredientRotationSpeed.tickChaser();
			ingredientRotation.setValue(ingredientRotation.getValue() + ingredientRotationSpeed.getValue());
		}

		if ((!spoutputBuffer.isEmpty() || !spoutputFluidBuffer.isEmpty()) && !level.isClientSide)
			tryClearingSpoutputOverflow();
		if (!contentsChanged)
			return;

		contentsChanged = false;
		sendData();
		getOperator().ifPresent(be -> be.basinChecker.scheduleUpdate());

		for (Direction offset : Iterate.horizontalDirections) {
			BlockPos toUpdate = worldPosition.above()
				.relative(offset);
			BlockState stateToUpdate = level.getBlockState(toUpdate);
			if (stateToUpdate.getBlock() instanceof BasinBlock
				&& stateToUpdate.getValue(BasinBlock.FACING) == offset.getOpposite()) {
				BlockEntity be = level.getBlockEntity(toUpdate);
				if (be instanceof BasinBlockEntity)
					((BasinBlockEntity) be).contentsChanged = true;
			}
		}
	}

	private void tryClearingSpoutputOverflow() {
		BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof BasinBlock))
			return;
		Direction direction = blockState.getValue(BasinBlock.FACING);
		BlockEntity be = level.getBlockEntity(worldPosition.below()
			.relative(direction));

		FilteringBehaviour filter = null;
		InvManipulationBehaviour inserter = null;
		if (be != null) {
			filter = BlockEntityBehaviour.get(level, be.getBlockPos(), FilteringBehaviour.TYPE);
			inserter = BlockEntityBehaviour.get(level, be.getBlockPos(), InvManipulationBehaviour.TYPE);
		}

		Storage<ItemVariant> targetInv = getItemSpoutputOutput(direction);
		if (targetInv == null && inserter != null) targetInv = inserter.getInventory();

		Storage<FluidVariant> targetTank = getFluidSpoutputOutput(direction);

		boolean update = false;

		try (Transaction t = TransferUtil.getTransaction()) {
			for (Iterator<ItemStack> iterator = spoutputBuffer.iterator(); iterator.hasNext();) {
				ItemStack itemStack = iterator.next();
				// fabric: cleanup for #599
				if (itemStack.isEmpty())
					continue;

				if (direction == Direction.DOWN) {
					Block.popResource(level, worldPosition, itemStack);
					iterator.remove();
					update = true;
					continue;
				}

				if (targetInv == null)
					break;
				try (Transaction nested = t.openNested()) {
					long inserted = targetInv.insert(ItemVariant.of(itemStack), itemStack.getCount(), nested);
					if (itemStack.getCount() != inserted)
						continue;
					if (filter != null && !filter.test(itemStack))
						continue;

					update = true;
					iterator.remove();
					visualizedOutputItems.add(LongAttached.withZero(itemStack));
					nested.commit();
				}
			}

			for (Iterator<FluidStack> iterator = spoutputFluidBuffer.iterator(); iterator.hasNext();) {
				FluidStack fluidStack = iterator.next();

				if (direction == Direction.DOWN) {
					iterator.remove();
					update = true;
					continue;
				}

				if (targetTank == null)
					break;

				try (Transaction nested = t.openNested()) {
					long fill = targetTank instanceof SmartFluidTankBehaviour.InternalFluidHandler
							? ((SmartFluidTankBehaviour.InternalFluidHandler) targetTank).forceFill(fluidStack.copy(), nested)
							: targetTank.insert(fluidStack.getType(), fluidStack.getAmount(), nested);
					if (fill != fluidStack.getAmount())
						break;

					update = true;
					iterator.remove();
					visualizedOutputFluids.add(LongAttached.withZero(fluidStack));
					nested.commit();
				}
			}

			if (update) {
				notifyChangeOfContents();
				sendData();
			}
			t.commit();
		}
	}

	public float getTotalFluidUnits(float partialTicks) {
		int renderedFluids = 0;
		float totalUnits = 0;

		for (SmartFluidTankBehaviour behaviour : getTanks()) {
			if (behaviour == null)
				continue;
			for (TankSegment tankSegment : behaviour.getTanks()) {
				if (tankSegment.getRenderedFluid()
					.isEmpty())
					continue;
				float units = tankSegment.getTotalUnits(partialTicks);
				if (units < 1)
					continue;
				totalUnits += units;
				renderedFluids++;
			}
		}

		if (renderedFluids == 0)
			return 0;
		if (totalUnits < 1)
			return 0;
		return totalUnits;
	}

	private Optional<BasinOperatingBlockEntity> getOperator() {
		if (level == null)
			return Optional.empty();
		BlockEntity be = level.getBlockEntity(worldPosition.above(2));
		if (be instanceof BasinOperatingBlockEntity)
			return Optional.of((BasinOperatingBlockEntity) be);
		return Optional.empty();
	}

	public FilteringBehaviour getFilter() {
		return filtering;
	}

	public void notifyChangeOfContents() {
		contentsChanged = true;
	}

	public SmartInventory getInputInventory() {
		return inputInventory;
	}

	public SmartInventory getOutputInventory() {
		return outputInventory;
	}

	public boolean canContinueProcessing() {
		return spoutputBuffer.isEmpty() && spoutputFluidBuffer.isEmpty();
	}

	public boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, TransactionContext ctx) {
		outputInventory.allowInsertion();
		outputTank.allowInsertion();
		boolean acceptOutputsInner = acceptOutputsInner(outputItems, outputFluids, ctx);
		outputInventory.forbidInsertion();
		outputTank.forbidInsertion();
		return acceptOutputsInner;
	}

	private boolean acceptOutputsInner(List<ItemStack> outputItems, List<FluidStack> outputFluids, TransactionContext ctx) {
		BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof BasinBlock))
			return false;

		Direction direction = blockState.getValue(BasinBlock.FACING);
		snapshotParticipant.updateSnapshots(ctx);
		if (direction != Direction.DOWN) {

			BlockEntity be = level.getBlockEntity(worldPosition.below()
					.relative(direction));

			InvManipulationBehaviour inserter =
					be == null ? null : BlockEntityBehaviour.get(level, be.getBlockPos(), InvManipulationBehaviour.TYPE);
			Storage<ItemVariant> targetInv = getItemSpoutputOutput(direction);
			if (targetInv == null && inserter != null) targetInv = inserter.getInventory();
			Storage<FluidVariant> targetTank = getFluidSpoutputOutput(direction);
			boolean externalTankNotPresent = targetTank == null;

			if (!outputItems.isEmpty() && targetInv == null)
				return false;
			if (!outputFluids.isEmpty() && externalTankNotPresent) {
				// Special case - fluid outputs but output only accepts items
				targetTank = outputTank.getCapability();
				if (targetTank == null)
					return false;
				if (!acceptFluidOutputsIntoBasin(outputFluids, ctx, targetTank))
					return false;
			}

			for (ItemStack itemStack : outputItems) {
				spoutputBuffer.add(itemStack.copy());
			}
			if (!externalTankNotPresent)
				for (FluidStack fluidStack : outputFluids)
					spoutputFluidBuffer.add(fluidStack.copy());
			return true;
		}

		Storage<ItemVariant> targetInv = outputInventory;
		Storage<FluidVariant> targetTank = outputTank.getCapability();

		if (targetInv == null && !outputItems.isEmpty())
			return false;
		if (!acceptItemOutputsIntoBasin(outputItems, ctx, targetInv))
			return false;
		if (outputFluids.isEmpty())
			return true;
		if (targetTank == null)
			return false;
		if (!acceptFluidOutputsIntoBasin(outputFluids, ctx, targetTank))
			return false;

		return true;
	}

	private boolean acceptFluidOutputsIntoBasin(List<FluidStack> outputFluids, TransactionContext ctx,
		Storage<FluidVariant> targetTank) {
		for (FluidStack fluidStack : outputFluids) {
			long fill = targetTank instanceof SmartFluidTankBehaviour.InternalFluidHandler
				? ((SmartFluidTankBehaviour.InternalFluidHandler) targetTank).forceFill(fluidStack.copy(), ctx)
				: targetTank.insert(fluidStack.getType(), fluidStack.getAmount(), ctx);
			if (fill != fluidStack.getAmount())
				return false;
		}
		return true;
	}

	private boolean acceptItemOutputsIntoBasin(List<ItemStack> outputItems, TransactionContext ctx, Storage<ItemVariant> targetInv) {
		try (Transaction t = ctx.openNested()) {
			for (ItemStack itemStack : outputItems) {
				long inserted = targetInv.insert(ItemVariant.of(itemStack), itemStack.getCount(), t);
				if (inserted != itemStack.getCount()) {
					t.commit();
					return false;
				}
			}
			t.commit();
		}
		return true;
	}

	public void readOnlyItems(CompoundTag compound) {
		inputInventory.deserializeNBT(compound.getCompound("InputItems"));
		outputInventory.deserializeNBT(compound.getCompound("OutputItems"));
	}

	public static HeatLevel getHeatLevelOf(BlockState state) {
		if (state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL))
			return state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
		return AllTags.AllBlockTags.PASSIVE_BOILER_HEATERS.matches(state) && BlockHelper.isNotUnheated(state) ? HeatLevel.SMOULDERING : HeatLevel.NONE;
	}

	public Couple<SmartFluidTankBehaviour> getTanks() {
		return tanks;
	}

	public Couple<SmartInventory> getInvs() {
		return invs;
	}

	// client things

	private void tickVisualizedOutputs() {
		visualizedOutputFluids.forEach(LongAttached::decrement);
		visualizedOutputItems.forEach(LongAttached::decrement);
		visualizedOutputFluids.removeIf(LongAttached::isOrBelowZero);
		visualizedOutputItems.removeIf(LongAttached::isOrBelowZero);
	}

	private void createFluidParticles() {
		RandomSource r = level.random;

		if (!visualizedOutputFluids.isEmpty())
			createOutputFluidParticles(r);

		if (!areFluidsMoving && r.nextFloat() > 1 / 8f)
			return;

		int segments = 0;
		for (SmartFluidTankBehaviour behaviour : getTanks()) {
			if (behaviour == null)
				continue;
			for (TankSegment tankSegment : behaviour.getTanks())
				if (!tankSegment.isEmpty(0))
					segments++;
		}
		if (segments < 2)
			return;

		float totalUnits = getTotalFluidUnits(0);
		if (totalUnits == 0)
			return;
		float fluidLevel = Mth.clamp(totalUnits / 2000, 0, 1);
		float rim = 2 / 16f;
		float space = 12 / 16f;
		float surface = worldPosition.getY() + rim + space * fluidLevel + 1 / 32f;

		if (areFluidsMoving) {
			createMovingFluidParticles(surface, segments);
			return;
		}

		for (SmartFluidTankBehaviour behaviour : getTanks()) {
			if (behaviour == null)
				continue;
			for (TankSegment tankSegment : behaviour.getTanks()) {
				if (tankSegment.isEmpty(0))
					continue;
				float x = worldPosition.getX() + rim + space * r.nextFloat();
				float z = worldPosition.getZ() + rim + space * r.nextFloat();
				level.addAlwaysVisibleParticle(
					new FluidParticleData(AllParticleTypes.BASIN_FLUID.get(), tankSegment.getRenderedFluid()), x,
					surface, z, 0, 0, 0);
			}
		}
	}

	private void createOutputFluidParticles(RandomSource r) {
		BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof BasinBlock))
			return;
		Direction direction = blockState.getValue(BasinBlock.FACING);
		if (direction == Direction.DOWN)
			return;
		Vec3 directionVec = Vec3.atLowerCornerOf(direction.getNormal());
		Vec3 outVec = VecHelper.getCenterOf(worldPosition)
			.add(directionVec.scale(.65)
				.subtract(0, 1 / 4f, 0));
		Vec3 outMotion = directionVec.scale(1 / 16f)
			.add(0, -1 / 16f, 0);

		for (int i = 0; i < 2; i++) {
			visualizedOutputFluids.forEach(ia -> {
				FluidStack fluidStack = ia.getValue();
				ParticleOptions fluidParticle = FluidFX.getFluidParticle(fluidStack);
				Vec3 m = VecHelper.offsetRandomly(outMotion, r, 1 / 16f);
				level.addAlwaysVisibleParticle(fluidParticle, outVec.x, outVec.y, outVec.z, m.x, m.y, m.z);
			});
		}
	}

	private void createMovingFluidParticles(float surface, int segments) {
		Vec3 pointer = new Vec3(1, 0, 0).scale(1 / 16f);
		float interval = 360f / segments;
		Vec3 centerOf = VecHelper.getCenterOf(worldPosition);
		float intervalOffset = (AnimationTickHolder.getTicks() * 18) % 360;

		int currentSegment = 0;
		for (SmartFluidTankBehaviour behaviour : getTanks()) {
			if (behaviour == null)
				continue;
			for (TankSegment tankSegment : behaviour.getTanks()) {
				if (tankSegment.isEmpty(0))
					continue;
				float angle = interval * (1 + currentSegment) + intervalOffset;
				Vec3 vec = centerOf.add(VecHelper.rotate(pointer, angle, Axis.Y));
				level.addAlwaysVisibleParticle(
					new FluidParticleData(AllParticleTypes.BASIN_FLUID.get(), tankSegment.getRenderedFluid()), vec.x(),
					surface, vec.z(), 1, 0, 0);
				currentSegment++;
			}
		}
	}

	public boolean areFluidsMoving() {
		return areFluidsMoving;
	}

	public boolean setAreFluidsMoving(boolean areFluidsMoving) {
		this.areFluidsMoving = areFluidsMoving;
		ingredientRotationSpeed.chase(areFluidsMoving ? 20 : 0, .1f, Chaser.EXP);
		return areFluidsMoving;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		Lang.translate("gui.goggles.basin_contents")
			.forGoggles(tooltip);

		boolean isEmpty = true;

		for (SmartInventory inventory : invs) {
			for (int i = 0; i < inventory.getSlotCount(); i++) {
				ItemStack stackInSlot = inventory.getStackInSlot(i);
				if (stackInSlot.isEmpty())
					continue;
				Lang.text("")
						.add(Components.translatable(stackInSlot.getDescriptionId())
								.withStyle(ChatFormatting.GRAY))
						.add(Lang.text(" x" + stackInSlot.getCount())
								.style(ChatFormatting.GREEN))
						.forGoggles(tooltip, 1);
				isEmpty = false;
			}
		}

		FluidUnit unit = AllConfigs.client().fluidUnitType.get();
		LangBuilder unitSuffix = Lang.translate(unit.getTranslationKey());
		boolean simplify = AllConfigs.client().simplifyFluidUnit.get();
		for (SmartFluidTankBehaviour behaviour : tanks) {
			for (TankSegment tank : behaviour.getTanks()) {
				FluidStack fluidStack = tank.getTank().getFluid();
				if (fluidStack.isEmpty())
					continue;
				Lang.text("")
						.add(Lang.fluidName(fluidStack)
								.add(Lang.text(" "))
								.style(ChatFormatting.GRAY)
								.add(Lang.text(FluidTextUtil.getUnicodeMillibuckets(fluidStack.getAmount(), unit, simplify))
										.add(unitSuffix)
										.style(ChatFormatting.BLUE)))
						.forGoggles(tooltip, 1);
				isEmpty = false;
			}
		}


		if (isEmpty)
			tooltip.remove(0);

		return true;
	}

	@Nullable
	@Override
	public Storage<FluidVariant> getFluidStorage(@Nullable Direction face) {
		return fluidCapability;
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		return itemCapability;
	}

	class BasinValueBox extends ValueBoxTransform.Sided {

		@Override
		protected Vec3 getSouthLocation() {
			return VecHelper.voxelSpace(8, 12, 16.05);
		}

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			return direction.getAxis()
				.isHorizontal();
		}

	}
}
