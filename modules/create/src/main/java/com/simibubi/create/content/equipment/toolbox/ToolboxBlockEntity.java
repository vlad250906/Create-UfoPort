package com.simibubi.create.content.equipment.toolbox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.ResetableLazy;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ToolboxBlockEntity extends SmartBlockEntity implements MenuProvider, Nameable, SidedStorageBlockEntity {

	public LerpedFloat lid = LerpedFloat.linear()
		.startWithValue(0);

	public LerpedFloat drawers = LerpedFloat.linear()
		.startWithValue(0);

	UUID uniqueId;
	ToolboxInventory inventory;
	ResetableLazy<DyeColor> colorProvider;
	protected int openCount;

	Map<Integer, WeakHashMap<Player, Integer>> connectedPlayers;

	private Component customName;

	public ToolboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		connectedPlayers = new HashMap<>();
		inventory = new ToolboxInventory(this);
		colorProvider = ResetableLazy.of(() -> {
			BlockState blockState = getBlockState();
			if (blockState != null && blockState.getBlock() instanceof ToolboxBlock)
				return ((ToolboxBlock) blockState.getBlock()).getColor();
			return DyeColor.BROWN;
		});
		setLazyTickRate(10);
	}

	public DyeColor getColor() {
		return colorProvider.get();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	public void initialize() {
		super.initialize();
		ToolboxHandler.onLoad(this);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		ToolboxHandler.onUnload(this);
	}

	@Override
	public void tick() {
		super.tick();

		if (level.isClientSide)
			tickAudio();
		if (!level.isClientSide)
			tickPlayers();

		lid.chase(openCount > 0 ? 1 : 0, 0.2f, Chaser.LINEAR);
		drawers.chase(openCount > 0 ? 1 : 0, 0.2f, Chaser.EXP);
		lid.tickChaser();
		drawers.tickChaser();
	}

	private void tickPlayers() {
		boolean update = false;

		for (Iterator<Entry<Integer, WeakHashMap<Player, Integer>>> toolboxSlots = connectedPlayers.entrySet()
			.iterator(); toolboxSlots.hasNext();) {

			Entry<Integer, WeakHashMap<Player, Integer>> toolboxSlotEntry = toolboxSlots.next();
			WeakHashMap<Player, Integer> set = toolboxSlotEntry.getValue();
			int slot = toolboxSlotEntry.getKey();

			ItemStack referenceItem = inventory.filters.get(slot);
			boolean clear = referenceItem.isEmpty();

			for (Iterator<Entry<Player, Integer>> playerEntries = set.entrySet()
				.iterator(); playerEntries.hasNext();) {
				Entry<Player, Integer> playerEntry = playerEntries.next();

				Player player = playerEntry.getKey();
				int hotbarSlot = playerEntry.getValue();

				if (!clear && !ToolboxHandler.withinRange(player, this))
					continue;

				Inventory playerInv = player.getInventory();
				ItemStack playerStack = playerInv.getItem(hotbarSlot);

				if (clear || !playerStack.isEmpty()
					&& !ToolboxInventory.canItemsShareCompartment(playerStack, referenceItem)) {
					player.getCustomData()
						.getCompound("CreateToolboxData")
						.remove(String.valueOf(hotbarSlot));
					playerEntries.remove();
					if (player instanceof ServerPlayer)
						ToolboxHandler.syncData(player);
					continue;
				}

				int count = playerStack.getCount();
				int targetAmount = (referenceItem.getMaxStackSize() + 1) / 2;

				if (count < targetAmount) {
					int amountToReplenish = targetAmount - count;

					if (isOpenInContainer(player)) {
						try (Transaction t = TransferUtil.getTransaction()) {
							ItemStack extracted = inventory.takeFromCompartment(amountToReplenish, slot, t);
							if (!extracted.isEmpty()) {
								ToolboxHandler.unequip(player, hotbarSlot, false);
								ToolboxHandler.syncData(player);
								continue;
							}
						}
					}

					try (Transaction t = TransferUtil.getTransaction()) {
						ItemStack extracted = inventory.takeFromCompartment(amountToReplenish, slot, t);
						if (!extracted.isEmpty()) {
							update = true;
							ItemStack template = playerStack.isEmpty() ? extracted : playerStack;
							playerInv.setItem(hotbarSlot,
									ItemHandlerHelper.copyStackWithSize(template, count + extracted.getCount()));
							t.commit();
						}
					}
				}

				if (count > targetAmount) {
					int amountToDeposit = count - targetAmount;
					ItemStack toDistribute = ItemHandlerHelper.copyStackWithSize(playerStack, amountToDeposit);

					if (isOpenInContainer(player)) {
						try (Transaction t = TransferUtil.getTransaction()) {
							int deposited = amountToDeposit - inventory.distributeToCompartment(toDistribute, slot, t)
									.getCount();
							if (deposited > 0) {
								ToolboxHandler.unequip(player, hotbarSlot, true);
								ToolboxHandler.syncData(player);
								continue;
							}
						}

					}

					try (Transaction t = TransferUtil.getTransaction()) {
						int deposited = amountToDeposit - inventory.distributeToCompartment(toDistribute, slot, t)
								.getCount();
						if (deposited > 0) {
							update = true;
							playerInv.setItem(hotbarSlot,
									ItemHandlerHelper.copyStackWithSize(playerStack, count - deposited));
							t.commit();
						}
					}
				}
			}

			if (clear)
				toolboxSlots.remove();
		}

		if (update)

			sendData();

	}

	private boolean isOpenInContainer(Player player) {
		return player.containerMenu instanceof ToolboxMenu
			&& ((ToolboxMenu) player.containerMenu).contentHolder == this;
	}

	public void unequipTracked() {
		if (level.isClientSide)
			return;

		Set<ServerPlayer> affected = new HashSet<>();

		for (Iterator<Entry<Integer, WeakHashMap<Player, Integer>>> toolboxSlots = connectedPlayers.entrySet()
			.iterator(); toolboxSlots.hasNext();) {

			Entry<Integer, WeakHashMap<Player, Integer>> toolboxSlotEntry = toolboxSlots.next();
			WeakHashMap<Player, Integer> set = toolboxSlotEntry.getValue();

			for (Iterator<Entry<Player, Integer>> playerEntries = set.entrySet()
				.iterator(); playerEntries.hasNext();) {
				Entry<Player, Integer> playerEntry = playerEntries.next();

				Player player = playerEntry.getKey();
				int hotbarSlot = playerEntry.getValue();

				ToolboxHandler.unequip(player, hotbarSlot, false);
				if (player instanceof ServerPlayer)
					affected.add((ServerPlayer) player);
			}
		}

		for (ServerPlayer player : affected)
			ToolboxHandler.syncData(player);
		connectedPlayers.clear();
	}

	public void unequip(int slot, Player player, int hotbarSlot, boolean keepItems) {
		if (!connectedPlayers.containsKey(slot))
			return;
		connectedPlayers.get(slot)
			.remove(player);
		if (keepItems)
			return;

		PlayerInventoryStorage playerInv = PlayerInventoryStorage.of(player);
		SingleSlotStorage<ItemVariant> storage = playerInv.getSlot(hotbarSlot);
		if (storage.isResourceBlank())
			return;
		ItemVariant resource = storage.getResource();
		int amount = (int) storage.getAmount();
		ItemStack toInsert = ToolboxInventory.cleanItemNBT(resource.toStack(amount));
		try (Transaction t = TransferUtil.getTransaction()) {
			ItemStack remainder = inventory.distributeToCompartment(toInsert, slot, t);
			int inserted = amount - remainder.getCount();
			storage.extract(resource, inserted, t);
			t.commit();
		}
	}

	private void tickAudio() {
		Vec3 vec = VecHelper.getCenterOf(worldPosition);
		if (lid.settled()) {
			if (openCount > 0 && lid.getChaseTarget() == 0) {
				level.playLocalSound(vec.x, vec.y, vec.z, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.25F,
					level.random.nextFloat() * 0.1F + 1.2F, true);
				level.playLocalSound(vec.x, vec.y, vec.z, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.1F,
					level.random.nextFloat() * 0.1F + 1.1F, true);
			}
			if (openCount == 0 && lid.getChaseTarget() == 1)
				level.playLocalSound(vec.x, vec.y, vec.z, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.1F,
					level.random.nextFloat() * 0.1F + 1.1F, true);

		} else if (openCount == 0 && lid.getChaseTarget() == 0 && lid.getValue(0) > 1 / 16f
			&& lid.getValue(1) < 1 / 16f)
			level.playLocalSound(vec.x, vec.y, vec.z, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.25F,
				level.random.nextFloat() * 0.1F + 1.2F, true);
	}

	@Nullable
	@Override
	public Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
		return inventory;
	}
	
	@Override
	protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
		inventory.deserializeNBT(compound.getCompound("Inventory"));
		super.read(compound, registries, clientPacket);
		if (compound.contains("UniqueId", 11))
			this.uniqueId = compound.getUUID("UniqueId");
		if (compound.contains("CustomName", 8))
			this.customName = Component.Serializer.fromJson(compound.getString("CustomName"), Create.getRegistryAccess());
		if (clientPacket)
			openCount = compound.getInt("OpenCount");
	}

	@Override
	protected void write(CompoundTag compound, boolean clientPacket) {
		if (uniqueId == null)
			uniqueId = UUID.randomUUID();

		compound.put("Inventory", inventory.serializeNBT());
		compound.putUUID("UniqueId", uniqueId);

		if (customName != null)
			compound.putString("CustomName", Component.Serializer.toJson(customName, Create.getRegistryAccess()));
		super.write(compound, clientPacket);
		if (clientPacket)
			compound.putInt("OpenCount", openCount);
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return ToolboxMenu.create(id, inv, this);
	}

	@Override
	public void lazyTick() {
		updateOpenCount();
		// keep re-advertising active TEs
		ToolboxHandler.onLoad(this);
		super.lazyTick();
	}

	void updateOpenCount() {
		if (level.isClientSide)
			return;
		if (openCount == 0)
			return;

		int prevOpenCount = openCount;
		openCount = 0;

		for (Player playerentity : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(8)))
			if (playerentity.containerMenu instanceof ToolboxMenu
				&& ((ToolboxMenu) playerentity.containerMenu).contentHolder == this)
				openCount++;

		if (prevOpenCount != openCount)
			sendData();
	}

	public void startOpen(Player player) {
		if (player.isSpectator())
			return;
		if (openCount < 0)
			openCount = 0;
		openCount++;
		sendData();
	}

	public void stopOpen(Player player) {
		if (player.isSpectator())
			return;
		openCount--;
		sendData();
	}

	public void connectPlayer(int slot, Player player, int hotbarSlot) {
		if (level.isClientSide)
			return;
		WeakHashMap<Player, Integer> map = connectedPlayers.computeIfAbsent(slot, WeakHashMap::new);
		Integer previous = map.get(player);
		if (previous != null) {
			if (previous == hotbarSlot)
				return;
			ToolboxHandler.unequip(player, previous, false);
		}
		map.put(player, hotbarSlot);
	}

	public void readInventory(CompoundTag compound) {
		inventory.deserializeNBT(compound);
	}

	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public boolean isFullyInitialized() {
		// returns true when uniqueId has been initialized
		return uniqueId != null;
	}

	public void setCustomName(Component customName) {
		this.customName = customName;
	}

	@Override
	public Component getDisplayName() {
		return customName != null ? customName
			: AllBlocks.TOOLBOXES.get(getColor())
				.get()
				.getName();
	}

	@Override
	public Component getCustomName() {
		return customName;
	}

	@Override
	public boolean hasCustomName() {
		return customName != null;
	}

	@Override
	public Component getName() {
		return customName;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setBlockState(BlockState state) {
		super.setBlockState(state);
		colorProvider.reset();
	}
	
	@Override
	public void syncComponents() {
		if (uniqueId == null)
			uniqueId = UUID.randomUUID();

		CompoundTag compound = new CompoundTag();
		compound.put("Inventory", inventory.serializeNBT());
		compound.putUUID("UniqueId", uniqueId);
		
		DataComponentPatch.Builder builder = DataComponentPatch.builder();
		super.components().forEach(type -> builder.set((DataComponentType)type.type(), type.value()));
		builder.set(AllDataComponents.TOOLBOX, compound);
		super.applyComponents(super.components(), builder.build());
	}

}
