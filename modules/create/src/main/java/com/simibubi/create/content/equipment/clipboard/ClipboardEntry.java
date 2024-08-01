package com.simibubi.create.content.equipment.clipboard;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.NBTHelper;

import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class ClipboardEntry {

	public boolean checked;
	public MutableComponent text;
	public ItemStack icon;

	public ClipboardEntry(boolean checked, MutableComponent text) {
		this.checked = checked;
		this.text = text;
		this.icon = ItemStack.EMPTY;
	}

	public ClipboardEntry displayItem(ItemStack icon) {
		this.icon = icon;
		return this;
	}

	public static List<List<ClipboardEntry>> readAll(ItemStack clipboardItem) {
		CompoundTag tag = clipboardItem.getOrDefault(AllDataComponents.CLIPBOARD_EDITING, null);
		if (tag == null)
			return new ArrayList<>();
		return NBTHelper.readCompoundList(tag.getList("Pages", Tag.TAG_COMPOUND), pageTag -> NBTHelper
			.readCompoundList(pageTag.getList("Entries", Tag.TAG_COMPOUND), ClipboardEntry::readNBT));
	}

	public static List<ClipboardEntry> getLastViewedEntries(ItemStack heldItem) {
		List<List<ClipboardEntry>> pages = ClipboardEntry.readAll(heldItem);
		if (pages.isEmpty())
			return new ArrayList<>();
		int page = heldItem.has(AllDataComponents.CLIPBOARD_EDITING) ? 0
			: Math.min(heldItem.get(AllDataComponents.CLIPBOARD_EDITING)
				.getInt("PreviouslyOpenedPage"), pages.size() - 1);
		List<ClipboardEntry> entries = pages.get(page);
		return entries;
	}

	public static void saveAll(List<List<ClipboardEntry>> entries, ItemStack clipboardItem) {
		CompoundTag tag = ItemHelper.getOrCreateComponent(clipboardItem, AllDataComponents.CLIPBOARD_EDITING, new CompoundTag());
		tag.put("Pages", NBTHelper.writeCompoundList(entries, list -> {
			CompoundTag pageTag = new CompoundTag();
			pageTag.put("Entries", NBTHelper.writeCompoundList(list, ClipboardEntry::writeNBT));
			return pageTag;
		}));
	}

	public CompoundTag writeNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean("Checked", checked);
		nbt.putString("Text", Component.Serializer.toJson(text, Create.getRegistryAccess()));
		if (icon.isEmpty())
			return nbt;
		nbt.put("Icon", NBTSerializer.serializeNBT(icon));
		return nbt;
	}

	public static ClipboardEntry readNBT(CompoundTag tag) {
		ClipboardEntry clipboardEntry =
			new ClipboardEntry(tag.getBoolean("Checked"), Component.Serializer.fromJson(tag.getString("Text"), Create.getRegistryAccess()));
		if (tag.contains("Icon"))
			clipboardEntry.displayItem(ItemStack.parseOptional(Create.getRegistryAccess(), tag.getCompound("Icon")));
		return clipboardEntry;
	}

}
