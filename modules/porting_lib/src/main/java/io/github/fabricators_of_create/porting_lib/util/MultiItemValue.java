package io.github.fabricators_of_create.porting_lib.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient.ItemValue;
import net.minecraft.world.item.crafting.Ingredient.TagValue;
import net.minecraft.world.item.crafting.Ingredient.Value;

public class MultiItemValue implements Value {
	private Collection<ItemStack> items;
	
	public static final Codec<MultiItemValue> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStack.CODEC.listOf().fieldOf("items").forGetter(miv -> new ArrayList<ItemStack>(miv.getItems()))
	).apply(instance, MultiItemValue::new));

	
	public MultiItemValue(Collection<ItemStack> items) {
		this.items = Collections.unmodifiableCollection(items);
	}

	@Override
	public Collection<ItemStack> getItems() {
		return items;
	}

	public JsonObject serialize() {
		JsonObject obj = CODEC.encodeStart(JsonOps.INSTANCE, this).result().get().getAsJsonObject();
		return obj;
	}

}
