package com.simibubi.create.content.schematics.cannon;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CSchematics;

import io.github.fabricators_of_create.porting_lib.block.CustomRenderBoundingBoxBlockEntity;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import io.github.fabricators_of_create.porting_lib.util.StorageProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

public class SchematicannonBlockEntity extends SmartBlockEntity implements MenuProvider, CustomRenderBoundingBoxBlockEntity {

	public static final int NEIGHBOUR_CHECKING = 100;
	public static final int MAX_ANCHOR_DISTANCE = 256;

	// Inventory
	public SchematicannonInventory inventory;

	public boolean sendUpdate;
	// Sync
	public boolean dontUpdateChecklist;
	public int neighbourCheckCooldown;

	// Printer
	public SchematicPrinter printer;
	public ItemStack missingItem;
	public boolean positionNotLoaded;
	public boolean hasCreativeCrate;
	private int printerCooldown;
	private int skipsLeft;
	private boolean blockSkipped;

	public BlockPos previousTarget;
	public List<LaunchedItem> flyingBlocks;
	public MaterialChecklist checklist;

	// Gui information
	public float fuelLevel;
	public float bookPrintingProgress;
	public float schematicProgress;
	public String statusMsg;
	public State state;
	public int blocksPlaced;
	public int blocksToPlace;

	// Settings
	public int replaceMode;
	public boolean skipMissing;
	public boolean replaceBlockEntities;

	// Render
	public boolean firstRenderTick;
	public float defaultYaw;

	// fabric: transfer
	private final Map<Direction, StorageProvider<ItemVariant>> storages = new HashMap<>();

	public SchematicannonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(30);
		flyingBlocks = new LinkedList<>();
		inventory = new SchematicannonInventory(this);
		statusMsg = "idle";
		this.state = State.STOPPED;
		replaceMode = 2;
		checklist = new MaterialChecklist();
		printer = new SchematicPrinter();
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		for (Direction direction : Iterate.directions) {
			BlockPos pos = worldPosition.relative(direction);
			StorageProvider<ItemVariant> provider = StorageProvider.createForItems(level, pos);
			storages.put(direction, provider);
		}
	}

	// fabric: storages not stored, only check for creative crate
	public void findInventories() {
		hasCreativeCrate = false;

		for (Direction facing : Iterate.directions) {
			BlockPos pos = worldPosition.relative(facing);

			if (!level.isLoaded(pos))
				continue;

			if (AllBlocks.CREATIVE_CRATE.has(level.getBlockState(pos)))
				hasCreativeCrate = true;
		}
	}

	@Override
	protected void read(CompoundTag compound, Provider reg, boolean clientPacket) {
		if (!clientPacket) {
			inventory.deserializeNBT(compound.getCompound("Inventory"));
		}

		// Gui information
		statusMsg = compound.getString("Status");
		schematicProgress = compound.getFloat("Progress");
		bookPrintingProgress = compound.getFloat("PaperProgress");
		fuelLevel = compound.getFloat("Fuel");
		String stateString = compound.getString("State");
		state = stateString.isEmpty() ? State.STOPPED : State.valueOf(compound.getString("State"));
		blocksPlaced = compound.getInt("AmountPlaced");
		blocksToPlace = compound.getInt("AmountToPlace");

		missingItem = null;
		if (compound.contains("MissingItem"))
			missingItem = ItemStack.parseOptional(Create.getRegistryAccess(), compound.getCompound("MissingItem"));

		// Settings
		CompoundTag options = compound.getCompound("Options");
		replaceMode = options.getInt("ReplaceMode");
		skipMissing = options.getBoolean("SkipMissing");
		replaceBlockEntities = options.getBoolean("ReplaceTileEntities");

		// Printer & Flying Blocks
		if (compound.contains("Printer"))
			printer.fromTag(compound.getCompound("Printer"), clientPacket);
		if (compound.contains("FlyingBlocks"))
			readFlyingBlocks(compound);

		defaultYaw = compound.getFloat("DefaultYaw");

		super.read(compound, reg, clientPacket);
	}

	protected void readFlyingBlocks(CompoundTag compound) {
		ListTag tagBlocks = compound.getList("FlyingBlocks", 10);
		if (tagBlocks.isEmpty())
			flyingBlocks.clear();

		boolean pastDead = false;

		for (int i = 0; i < tagBlocks.size(); i++) {
			CompoundTag c = tagBlocks.getCompound(i);
			LaunchedItem launched = LaunchedItem.fromNBT(c, blockHolderGetter());
			BlockPos readBlockPos = launched.target;

			// Always write to Server block entity
			if (level == null || !level.isClientSide) {
				flyingBlocks.add(launched);
				continue;
			}

			// Delete all Client side blocks that are now missing on the server
			while (!pastDead && !flyingBlocks.isEmpty() && !flyingBlocks.get(0).target.equals(readBlockPos)) {
				flyingBlocks.remove(0);
			}

			pastDead = true;

			// Add new server side blocks
			if (i >= flyingBlocks.size()) {
				flyingBlocks.add(launched);
				continue;
			}

			// Don't do anything with existing
		}
	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		if (!clientPacket) {
			compound.put("Inventory", inventory.serializeNBT());
			if (state == State.RUNNING) {
				compound.putBoolean("Running", true);
			}
		}

		// Gui information
		compound.putFloat("Progress", schematicProgress);
		compound.putFloat("PaperProgress", bookPrintingProgress);
		compound.putFloat("Fuel", fuelLevel);
		compound.putString("Status", statusMsg);
		compound.putString("State", state.name());
		compound.putInt("AmountPlaced", blocksPlaced);
		compound.putInt("AmountToPlace", blocksToPlace);

		if (missingItem != null)
			compound.put("MissingItem", NBTSerializer.serializeNBT(missingItem));

		// Settings
		CompoundTag options = new CompoundTag();
		options.putInt("ReplaceMode", replaceMode);
		options.putBoolean("SkipMissing", skipMissing);
		options.putBoolean("ReplaceTileEntities", replaceBlockEntities);
		compound.put("Options", options);

		// Printer & Flying Blocks
		CompoundTag printerData = new CompoundTag();
		printer.write(printerData);
		compound.put("Printer", printerData);

		ListTag tagFlyingBlocks = new ListTag();
		for (LaunchedItem b : flyingBlocks)
			tagFlyingBlocks.add(b.serializeNBT());
		compound.put("FlyingBlocks", tagFlyingBlocks);

		compound.putFloat("DefaultYaw", defaultYaw);

		super.write(compound, clientPacket);
	}

	@Override
	public void tick() {
		super.tick();

		if (state != State.STOPPED && neighbourCheckCooldown-- <= 0) {
			neighbourCheckCooldown = NEIGHBOUR_CHECKING;
			findInventories();
		}

		firstRenderTick = true;
		previousTarget = printer.getCurrentTarget();
		tickFlyingBlocks();

		if (level.isClientSide)
			return;

		// Update Fuel and Paper
		tickPaperPrinter();
		refillFuelIfPossible();

		// Update Printer
		skipsLeft = 1000;
		blockSkipped = true;

		while (blockSkipped && skipsLeft-- > 0)
			tickPrinter();

		schematicProgress = 0;
		if (blocksToPlace > 0)
			schematicProgress = (float) blocksPlaced / blocksToPlace;

		// Update Client block entity
		if (sendUpdate) {
			sendUpdate = false;
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 6);
		}
	}

	public CSchematics config() {
		return AllConfigs.server().schematics;
	}

	protected void tickPrinter() {
		ItemStack blueprint = inventory.getStackInSlot(0);
		blockSkipped = false;

		if (blueprint.isEmpty() && !statusMsg.equals("idle") && inventory.getStackInSlot(1)
			.isEmpty()) {
			state = State.STOPPED;
			statusMsg = "idle";
			sendUpdate = true;
			return;
		}

		// Skip if not Active
		if (state == State.STOPPED) {
			if (printer.isLoaded())
				resetPrinter();
			return;
		}

		if (state == State.PAUSED && !positionNotLoaded && missingItem == null && fuelLevel > getFuelUsageRate())
			return;

		// Initialize Printer
		if (!printer.isLoaded()) {
			initializePrinter(blueprint);
			return;
		}

		// Cooldown from last shot
		if (printerCooldown > 0) {
			printerCooldown--;
			return;
		}

		// Check Fuel
		if (fuelLevel <= 0 && !hasCreativeCrate) {
			fuelLevel = 0;
			state = State.PAUSED;
			statusMsg = "noGunpowder";
			sendUpdate = true;
			return;
		}

		if (hasCreativeCrate) {
			if (missingItem != null) {
				missingItem = null;
				state = State.RUNNING;
			}
		}

		// Update Target
		if (missingItem == null && !positionNotLoaded) {
			if (!printer.advanceCurrentPos()) {
				finishedPrinting();
				return;
			}
			sendUpdate = true;
		}

		// Check block
		if (!getLevel().isLoaded(printer.getCurrentTarget())) {
			positionNotLoaded = true;
			statusMsg = "targetNotLoaded";
			state = State.PAUSED;
			return;
		} else {
			if (positionNotLoaded) {
				positionNotLoaded = false;
				state = State.RUNNING;
			}
		}

		// Get item requirement
		ItemRequirement requirement = printer.getCurrentRequirement();
		if (requirement.isInvalid() || !printer.shouldPlaceCurrent(level, this::shouldPlace)) {
			sendUpdate = !statusMsg.equals("searching");
			statusMsg = "searching";
			blockSkipped = true;
			return;
		}

		// Find item
		List<ItemRequirement.StackRequirement> requiredItems = requirement.getRequiredItems();
		if (!requirement.isEmpty()) {
			try (Transaction t = TransferUtil.getTransaction()) {
				for (ItemRequirement.StackRequirement required : requiredItems) {
					if (!grabItemsFromAttachedInventories(required, t)) {
						if (skipMissing) {
							statusMsg = "skipping";
							blockSkipped = true;
							if (missingItem != null) {
								missingItem = null;
								state = State.RUNNING;
							}
							return;
						}

						missingItem = required.stack;
						state = State.PAUSED;
						statusMsg = "missingBlock";
						return;
					}
				}

				t.commit();
			}
		}

		// Success
		state = State.RUNNING;
		ItemStack icon = requirement.isEmpty() || requiredItems.isEmpty() ? ItemStack.EMPTY : requiredItems.get(0).stack;
		printer.handleCurrentTarget((target, blockState, blockEntity) -> {
			// Launch block
			statusMsg = blockState.getBlock() != Blocks.AIR ? "placing" : "clearing";
			launchBlockOrBelt(target, icon, blockState, blockEntity);
		}, (target, entity) -> {
			// Launch entity
			statusMsg = "placing";
			launchEntity(target, icon, entity);
		});

		printerCooldown = config().schematicannonDelay.get();
		fuelLevel -= getFuelUsageRate();
		sendUpdate = true;
		missingItem = null;
	}

	public double getFuelUsageRate() {
		return hasCreativeCrate ? 0 : config().schematicannonFuelUsage.get() / 100f;
	}

	protected void initializePrinter(ItemStack blueprint) {
		if (!blueprint.has(AllDataComponents.SCHEMATIC_DATA)) {
			state = State.STOPPED;
			statusMsg = "schematicInvalid";
			sendUpdate = true;
			return;
		}

		if (!blueprint.get(AllDataComponents.SCHEMATIC_DATA)
				.getBoolean("Deployed")) {
			state = State.STOPPED;
			statusMsg = "schematicNotPlaced";
			sendUpdate = true;
			return;
		}

		// Load blocks into reader
		printer.loadSchematic(blueprint, level, true);

		if (printer.isErrored()) {
			state = State.STOPPED;
			statusMsg = "schematicErrored";
			inventory.setStackInSlot(0, ItemStack.EMPTY);
			inventory.setStackInSlot(1, new ItemStack(AllItems.EMPTY_SCHEMATIC.get()));
			printer.resetSchematic();
			sendUpdate = true;
			return;
		}

		if (printer.isWorldEmpty()) {
			state = State.STOPPED;
			statusMsg = "schematicExpired";
			inventory.setStackInSlot(0, ItemStack.EMPTY);
			inventory.setStackInSlot(1, new ItemStack(AllItems.EMPTY_SCHEMATIC.get()));
			printer.resetSchematic();
			sendUpdate = true;
			return;
		}

		if (!printer.getAnchor()
				.closerThan(getBlockPos(), MAX_ANCHOR_DISTANCE)) {
			state = State.STOPPED;
			statusMsg = "targetOutsideRange";
			printer.resetSchematic();
			sendUpdate = true;
			return;
		}

		state = State.PAUSED;
		statusMsg = "ready";
		updateChecklist();
		sendUpdate = true;
		blocksToPlace += blocksPlaced;
	}

	protected ItemStack getItemForBlock(BlockState blockState) {
		Item item = BlockItem.BY_BLOCK.getOrDefault(blockState.getBlock(), Items.AIR);
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	protected boolean grabItemsFromAttachedInventories(ItemRequirement.StackRequirement required, TransactionContext transaction) {
		if (hasCreativeCrate)
			return true;

		ItemStack stack = required.stack;
		ItemUseType usage = required.usage;

		// Find and apply damage
		// fabric - can't modify directly, extract and re-insert
		if (usage == ItemUseType.DAMAGE) {
			if (!stack.isDamageableItem())
				return false;

			for (Entry<Direction, StorageProvider<ItemVariant>> entry : storages.entrySet()) {
				Storage<ItemVariant> storage = entry.getValue().get(entry.getKey().getOpposite());
				if (storage == null)
					continue;
				try (Transaction t = transaction.openNested()) {
					ResourceAmount<ItemVariant> resource = TransferUtil.extractMatching(storage, required::matches, 1, t);
					if (resource == null || resource.amount() != 1)
						continue; // failed, skip
					ItemVariant variant = resource.resource();
					ItemStack newStack = variant.toStack();
					newStack.setDamageValue(newStack.getDamageValue() + 1);
					if (stack.getDamageValue() < stack.getMaxDamage()) {
						// stack not broken, re-insert
						ItemVariant newVariant = ItemVariant.of(newStack);
						long inserted = storage.insert(newVariant, 1, t);
						if (inserted != 1)
							continue; // failed to re-insert, cancel this whole attempt
					}
					t.commit();
					return true;
				}
			}
			// could not find in any storage
			return false;
		}

		// Find and remove
		int toExtract = stack.getCount();
		try (Transaction t = transaction.openNested()) {
			for (Entry<Direction, StorageProvider<ItemVariant>> entry : storages.entrySet()) {
				Storage<ItemVariant> storage = entry.getValue().get(entry.getKey().getOpposite());
				if (storage == null)
					continue;

				ResourceAmount<ItemVariant> resource = TransferUtil.extractMatching(storage, required::matches, stack.getCount(), t);
				if (resource == null)
					continue;
				toExtract -= resource.amount();
				if (toExtract > 0) // still need to extract more
					continue;
				// extracted enough
				t.commit();
				return true;
			}
		}
		// if we get here we didn't find enough
		return false;
	}

	public void finishedPrinting() {
		inventory.setStackInSlot(0, ItemStack.EMPTY);
		inventory.setStackInSlot(1, new ItemStack(AllItems.EMPTY_SCHEMATIC.get(), inventory.getStackInSlot(1)
				.getCount() + 1));
		state = State.STOPPED;
		statusMsg = "finished";
		resetPrinter();
		AllSoundEvents.SCHEMATICANNON_FINISH.playOnServer(level, worldPosition);
		sendUpdate = true;
	}

	protected void resetPrinter() {
		printer.resetSchematic();
		missingItem = null;
		sendUpdate = true;
		schematicProgress = 0;
		blocksPlaced = 0;
		blocksToPlace = 0;
	}

	protected boolean shouldPlace(BlockPos pos, BlockState state, BlockEntity be, BlockState toReplace,
								  BlockState toReplaceOther, boolean isNormalCube) {
		if (pos.closerThan(getBlockPos(), 2f))
			return false;
		if (!replaceBlockEntities
				&& (toReplace.hasBlockEntity() || (toReplaceOther != null && toReplaceOther.hasBlockEntity())))
			return false;

		if (shouldIgnoreBlockState(state, be))
			return false;

		boolean placingAir = state.isAir();

		if (replaceMode == 3)
			return true;
		if (replaceMode == 2 && !placingAir)
			return true;
		if (replaceMode == 1 && (isNormalCube || (!toReplace.isRedstoneConductor(level, pos)
				&& (toReplaceOther == null || !toReplaceOther.isRedstoneConductor(level, pos)))) && !placingAir)
			return true;
		if (replaceMode == 0 && !toReplace.isRedstoneConductor(level, pos)
				&& (toReplaceOther == null || !toReplaceOther.isRedstoneConductor(level, pos)) && !placingAir)
			return true;

		return false;
	}

	protected boolean shouldIgnoreBlockState(BlockState state, BlockEntity be) {
		// Block doesn't have a mapping (Water, lava, etc)
		if (state.getBlock() == Blocks.STRUCTURE_VOID)
			return true;

		ItemRequirement requirement = ItemRequirement.of(state, be);
		if (requirement.isEmpty())
			return false;
		if (requirement.isInvalid())
			return false;

		// Block doesn't need to be placed twice (Doors, beds, double plants)
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
				&& state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER)
			return true;
		if (state.hasProperty(BlockStateProperties.BED_PART)
				&& state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD)
			return true;
		if (state.getBlock() instanceof PistonHeadBlock)
			return true;
		if (AllBlocks.BELT.has(state))
			return state.getValue(BeltBlock.PART) == BeltPart.MIDDLE;

		return false;
	}

	protected void tickFlyingBlocks() {
		List<LaunchedItem> toRemove = new LinkedList<>();
		for (LaunchedItem b : flyingBlocks)
			if (b.update(level))
				toRemove.add(b);
		flyingBlocks.removeAll(toRemove);
	}

	protected void refillFuelIfPossible() {
		if (hasCreativeCrate)
			return;
		if (1 - fuelLevel + 1 / 128f < getFuelAddedByGunPowder())
			return;
		if (inventory.getStackInSlot(4)
				.isEmpty())
			return;

		inventory.getStackInSlot(4)
				.shrink(1);
		fuelLevel += getFuelAddedByGunPowder();
		if (statusMsg.equals("noGunpowder")) {
			if (blocksPlaced > 0)
				state = State.RUNNING;
			statusMsg = "ready";
		}
		sendUpdate = true;
	}

	public double getFuelAddedByGunPowder() {
		return config().schematicannonGunpowderWorth.get() / 100f;
	}

	protected void tickPaperPrinter() {
		int BookInput = 2;
		int BookOutput = 3;

		ItemStack blueprint = inventory.getStackInSlot(0);
		ItemStack paper = inventory.getStackInSlot(BookInput).copy();
		paper.setCount(1);
		boolean outputFull = inventory.getStackInSlot(BookOutput)
				.getCount() == inventory.getSlotLimit(BookOutput);
if (printer.isErrored())
			return;

		if (!printer.isLoaded()) {
			if (!blueprint.isEmpty())
				initializePrinter(blueprint);
			return;
		}

		if (paper.isEmpty() || outputFull) {
			if (bookPrintingProgress != 0)
				sendUpdate = true;
			bookPrintingProgress = 0;
			dontUpdateChecklist = false;
			return;
		}

		if (bookPrintingProgress >= 1) {
			bookPrintingProgress = 0;

			if (!dontUpdateChecklist)
				updateChecklist();

			dontUpdateChecklist = true;
			ItemStack extractItem = inventory.getStackInSlot(BookInput);
			// non-empty, would early exit above
			TransferUtil.extract(inventory, ItemVariant.of(extractItem), 1);
			ItemStack stack = AllBlocks.CLIPBOARD.isIn(extractItem) ? checklist.createWrittenClipboard()
				: checklist.createWrittenBook();
			stack.setCount(inventory.getStackInSlot(BookOutput)
					.getCount() + 1);
			inventory.setStackInSlot(BookOutput, stack);
			sendUpdate = true;
			return;
		}

		bookPrintingProgress += 0.05f;
		sendUpdate = true;
	}

	public static BlockState stripBeltIfNotLast(BlockState blockState) {
		BeltPart part = blockState.getValue(BeltBlock.PART);
		if (part == BeltPart.MIDDLE)
			return Blocks.AIR.defaultBlockState();

		// is highest belt?
		boolean isLastSegment = false;
		Direction facing = blockState.getValue(BeltBlock.HORIZONTAL_FACING);
		BeltSlope slope = blockState.getValue(BeltBlock.SLOPE);
		boolean positive = facing.getAxisDirection() == AxisDirection.POSITIVE;
		boolean start = part == BeltPart.START;
		boolean end = part == BeltPart.END;

		switch (slope) {
			case DOWNWARD:
				isLastSegment = start;
				break;
			case UPWARD:
				isLastSegment = end;
				break;
			default:
				isLastSegment = positive && end || !positive && start;
		}
		if (isLastSegment)
			return blockState;

		return AllBlocks.SHAFT.getDefaultState()
				.setValue(AbstractSimpleShaftBlock.AXIS, slope == BeltSlope.SIDEWAYS ? Axis.Y
						:facing.getClockWise()
								.getAxis());
	}

	protected void launchBlockOrBelt(BlockPos target, ItemStack icon, BlockState blockState, BlockEntity blockEntity) {
		if (AllBlocks.BELT.has(blockState)) {
			blockState = stripBeltIfNotLast(blockState);
			if (blockEntity instanceof BeltBlockEntity bbe && AllBlocks.BELT.has(blockState)) {
				CasingType[] casings = new CasingType[bbe.beltLength];
				Arrays.fill(casings, CasingType.NONE);
				BlockPos currentPos = target;
				for (int i = 0; i < bbe.beltLength; i++) {
					BlockState currentState = bbe.getLevel()
						.getBlockState(currentPos);
					if (!(currentState.getBlock() instanceof BeltBlock))
						break;
					if (!(bbe.getLevel()
						.getBlockEntity(currentPos) instanceof BeltBlockEntity beltAtSegment))
						break;
					casings[i] = beltAtSegment.casing;
					currentPos = BeltBlock.nextSegmentPosition(currentState, currentPos,
						blockState.getValue(BeltBlock.PART) != BeltPart.END);
				}
				launchBelt(target, blockState, bbe.beltLength, casings);
			} else if (blockState != Blocks.AIR.defaultBlockState())
				launchBlock(target, icon, blockState, null);
			return;
		}

		CompoundTag data = BlockHelper.prepareBlockEntityData(blockState, blockEntity);
		launchBlock(target, icon, blockState, data);
	}

	protected void launchBelt(BlockPos target, BlockState state, int length, CasingType[] casings) {
		blocksPlaced++;
		ItemStack connector = AllItems.BELT_CONNECTOR.asStack();
		flyingBlocks.add(new LaunchedItem.ForBelt(this.getBlockPos(), target, connector, state, casings));
		playFiringSound();
	}

	protected void launchBlock(BlockPos target, ItemStack stack, BlockState state, @Nullable CompoundTag data) {
		if (!state.isAir())
			blocksPlaced++;
		flyingBlocks.add(new LaunchedItem.ForBlockState(this.getBlockPos(), target, stack, state, data));
		playFiringSound();
	}

	protected void launchEntity(BlockPos target, ItemStack stack, Entity entity) {
		blocksPlaced++;
		flyingBlocks.add(new LaunchedItem.ForEntity(this.getBlockPos(), target, stack, entity));
		playFiringSound();
	}

	public void playFiringSound() {
		AllSoundEvents.SCHEMATICANNON_LAUNCH_BLOCK.playOnServer(level, worldPosition);
	}

	public void sendToMenu(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(getBlockPos());
		buffer.writeNbt(getUpdateTag(Create.getRegistryAccess()));
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return SchematicannonMenu.create(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return Lang.translateDirect("gui.schematicannon.title");
	}

	public void updateChecklist() {
		checklist.required.clear();
		checklist.damageRequired.clear();
		checklist.blocksNotLoaded = false;

		if (printer.isLoaded() && !printer.isErrored()) {
			blocksToPlace = blocksPlaced;
			blocksToPlace += printer.markAllBlockRequirements(checklist, level, this::shouldPlace);
			printer.markAllEntityRequirements(checklist);
		}

		checklist.gathered.clear();
		try (Transaction t = TransferUtil.getTransaction()) {
			for (Entry<Direction, StorageProvider<ItemVariant>> entry : storages.entrySet()) {
				Storage<ItemVariant> storage = entry.getValue().get(entry.getKey().getOpposite());
				if (storage == null)
					continue;
				storage.nonEmptyViews().forEach(checklist::collect);
			}
		}
		sendUpdate = true;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		findInventories();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public AABB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}
	
	@Override
	public void syncComponents() {
		CompoundTag compound = new CompoundTag();
		CompoundTag options = new CompoundTag();
		options.putInt("ReplaceMode", replaceMode);
		options.putBoolean("SkipMissing", skipMissing);
		options.putBoolean("ReplaceTileEntities", replaceBlockEntities);
		compound.put("Options", options);
		
		DataComponentPatch.Builder builder = DataComponentPatch.builder();
		super.components().forEach(type -> builder.set((DataComponentType)type.type(), type.value()));
		builder.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(compound));
		super.applyComponents(super.components(), builder.build());
	}

	public enum State {
		STOPPED, PAUSED, RUNNING;
	}

}
