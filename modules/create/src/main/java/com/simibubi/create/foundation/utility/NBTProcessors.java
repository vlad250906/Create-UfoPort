package com.simibubi.create.foundation.utility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.Create;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public final class NBTProcessors {

	private static final Map<BlockEntityType<?>, UnaryOperator<CompoundTag>> processors = new HashMap<>();
	private static final Map<BlockEntityType<?>, UnaryOperator<CompoundTag>> survivalProcessors = new HashMap<>();

	public static synchronized void addProcessor(BlockEntityType<?> type, UnaryOperator<CompoundTag> processor) {
		processors.put(type, processor);
	}

	public static synchronized void addSurvivalProcessor(BlockEntityType<?> type,
		UnaryOperator<CompoundTag> processor) {
		survivalProcessors.put(type, processor);
	}

	static {
		String[] signSides = new String[] { "front_text", "back_text" };
		UnaryOperator<CompoundTag> signProcessor = data -> {
			for (String side : signSides) {
				if (data.contains(side, Tag.TAG_COMPOUND)) {
					CompoundTag sideData = data.getCompound(side);
					if (sideData.contains("messages", Tag.TAG_LIST)) {
						ListTag messages = sideData.getList("messages", Tag.TAG_STRING);
						for (int i = 0; i < messages.size(); i++) {
							String string = messages.getString(i);
							if (textComponentHasClickEvent(string)) {
								return null;
							}
						}
					}
				}
			}
			return data;
		};
		addProcessor(BlockEntityType.SIGN, signProcessor);
		addProcessor(BlockEntityType.HANGING_SIGN, signProcessor);

		addProcessor(BlockEntityType.LECTERN, data -> {
			if (!data.contains("Book", Tag.TAG_COMPOUND))
				return data;
			CompoundTag book = data.getCompound("Book");

			if (!book.contains("tag", Tag.TAG_COMPOUND))
				return data;
			CompoundTag tag = book.getCompound("tag");

			if (!tag.contains("pages", Tag.TAG_LIST))
				return data;
			ListTag pages = tag.getList("pages", Tag.TAG_STRING);

			for (Tag inbt : pages) {
				if (textComponentHasClickEvent(inbt.getAsString()))
					return null;
			}
			return data;
		});
		addProcessor(AllBlockEntityTypes.CREATIVE_CRATE.get(), itemProcessor("Filter"));
		addProcessor(AllBlockEntityTypes.PLACARD.get(), itemProcessor("Item"));
	}

	public static UnaryOperator<CompoundTag> itemProcessor(String tagKey) {
		return data -> {
			CompoundTag compound = data.getCompound(tagKey);
			if (!compound.contains("tag", 10))
				return data;
			CompoundTag itemTag = compound.getCompound("tag");
			if (itemTag == null)
				return data;
			HashSet<String> keys = new HashSet<>(itemTag.getAllKeys());
			for (String key : keys)
				if (isUnsafeItemNBTKey(key))
					itemTag.remove(key);
			if (itemTag.isEmpty())
				compound.remove("tag");
			return data;
		};
	}

	public static ItemStack withUnsafeNBTDiscarded(ItemStack stack) {
		if (stack.getComponents().isEmpty())
			return stack;
		ItemStack copy = stack.copy();
		stack.getComponents()
			.keySet()
			.stream()
			.filter(NBTProcessors::isUnsafeItemNBTKey)
			.forEach(copy::remove);
		return copy;
	}

	public static boolean isUnsafeItemNBTKey(DataComponentType<?> name) {
		if (name.equals(DataComponents.STORED_ENCHANTMENTS))
			return false;
		if (name.equals(DataComponents.ENCHANTMENTS))
			return false;
		if (name.equals(DataComponents.POTION_CONTENTS))
			return false;
		if (name.equals(DataComponents.DAMAGE))
			return false;
		if (name.equals(DataComponents.MAX_DAMAGE))
			return false;
		if (name.equals(DataComponents.CUSTOM_NAME))
			return false;
		return true;
	}
	
	public static boolean isUnsafeItemNBTKey(String name) {
		if (name.equals("StoredEnchantments"))
			return false;
		if (name.equals("Enchantments"))
			return false;
		if (name.contains("Potion"))
			return false;
		if (name.contains("Damage"))
			return false;
		if (name.equals("display"))
			return false;
		return true;
	}

	public static boolean textComponentHasClickEvent(String json) {
		Component component = Component.Serializer.fromJson(json.isEmpty() ? "\"\"" : json, Create.getRegistryAccess());
		return component != null && component.getStyle() != null && component.getStyle()
			.getClickEvent() != null;
	}

	private NBTProcessors() {}

	@Nullable
	public static CompoundTag process(BlockEntity blockEntity, CompoundTag compound, boolean survival) {
		if (compound == null)
			return null;
		BlockEntityType<?> type = blockEntity.getType();
		if (survival && survivalProcessors.containsKey(type))
			compound = survivalProcessors.get(type)
				.apply(compound);
		if (compound != null && processors.containsKey(type))
			return processors.get(type)
				.apply(compound);
		if (blockEntity instanceof SpawnerBlockEntity)
			return compound;
		if (blockEntity.onlyOpCanSetNbt())
			return null;
		return compound;
	}

}
