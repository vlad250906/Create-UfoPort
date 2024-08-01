package com.simibubi.create.content.equipment.clipboard;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.item.ItemHelper;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile.UncheckedModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.item.ItemModelBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class ClipboardOverrides {

	public enum ClipboardType {
		EMPTY("empty_clipboard"), WRITTEN("clipboard"), EDITING("clipboard_and_quill");

		public String file;
		public static ResourceLocation ID = Create.asResource("clipboard_type");

		private ClipboardType(String file) {
			this.file = file;
		}
	}

	public static void switchTo(ClipboardType type, ItemStack clipboardItem) {
		CompoundTag tag = ItemHelper.getOrCreateComponent(clipboardItem, AllDataComponents.CLIPBOARD_EDITING, new CompoundTag());
		tag.putInt("Type", type.ordinal());
	}

	@Environment(EnvType.CLIENT)
	public static void registerModelOverridesClient(ClipboardBlockItem item) {
		ItemProperties.register(item, ClipboardType.ID, (pStack, pLevel, pEntity, pSeed) -> {
			CompoundTag tag = pStack.getOrDefault(AllDataComponents.CLIPBOARD_EDITING, null);
			return tag == null ? 0 : tag.getInt("Type");
		});
	}

	public static ItemModelBuilder addOverrideModels(DataGenContext<Item, ClipboardBlockItem> c,
		RegistrateItemModelProvider p) {
		ItemModelBuilder builder = p.generated(() -> c.get());
		for (int i = 0; i < ClipboardType.values().length; i++) {
			builder.override()
				.predicate(ClipboardType.ID, i)
				.model(p.getBuilder(c.getName() + "_" + i)
					.parent(new UncheckedModelFile("item/generated"))
					.texture("layer0", Create.asResource("item/" + ClipboardType.values()[i].file)))
				.end();
		}
		return builder;
	}

}
