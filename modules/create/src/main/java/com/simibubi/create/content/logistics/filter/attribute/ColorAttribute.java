package com.simibubi.create.content.logistics.filter.attribute;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.simibubi.create.content.logistics.filter.ItemAttribute;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import io.github.fabricators_of_create.porting_lib.util.TagUtil;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.FireworkStarItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;

public class ColorAttribute implements ItemAttribute {
	public static final ColorAttribute EMPTY = new ColorAttribute(DyeColor.PURPLE);

	public final DyeColor color;

	public ColorAttribute(DyeColor color) {
		this.color = color;
	}

	@Override
	public boolean appliesTo(ItemStack itemStack) {
		return findMatchingDyeColors(itemStack).stream().anyMatch(color::equals);
	}

	@Override
	public List<ItemAttribute> listAttributesOf(ItemStack itemStack) {
		return findMatchingDyeColors(itemStack).stream().map(ColorAttribute::new).collect(Collectors.toList());
	}

	private Collection<DyeColor> findMatchingDyeColors(ItemStack stack) {
		//CompoundTag nbt = stack.getTag();

		DyeColor color = TagUtil.getColorFromStack(stack);
		if (color != null)
			return Collections.singletonList(color);

		final Set<DyeColor> colors = new HashSet<>();
		if (stack.getItem() instanceof FireworkRocketItem && stack.has(DataComponents.FIREWORKS)) {
			stack.get(DataComponents.FIREWORKS).explosions().forEach(obj -> colors.addAll(getFireworkStarColors(obj)));
//			ListTag listnbt = nbt.getCompound("Fireworks").getList("Explosions", 10);
//			for (int i = 0; i < listnbt.size(); i++) {
//				colors.addAll(getFireworkStarColors(listnbt.getCompound(i)));
//			}
		}

		if (stack.getItem() instanceof FireworkStarItem && stack.has(DataComponents.FIREWORK_EXPLOSION)) {
			colors.addAll(getFireworkStarColors(stack.get(DataComponents.FIREWORK_EXPLOSION)));
		}

		Arrays.stream(DyeColor.values()).filter(c -> RegisteredObjects.getKeyOrThrow(stack.getItem()).getPath().startsWith(c.getName() + "_")).forEach(colors::add);

		return colors;
	}

	private Collection<DyeColor> getFireworkStarColors(FireworkExplosion compound) {
		final Set<DyeColor> colors = new HashSet<>();
		compound.colors().forEach(cnt -> colors.add(DyeColor.byFireworkColor(cnt)));
		compound.fadeColors().forEach(cnt -> colors.add(DyeColor.byFireworkColor(cnt)));
		
//		Arrays.stream(compound.getIntArray("Colors")).mapToObj(DyeColor::byFireworkColor).forEach(colors::add);
//		Arrays.stream(compound.getIntArray("FadeColors")).mapToObj(DyeColor::byFireworkColor).forEach(colors::add);
		return colors;
	}

	@Override
	public String getTranslationKey() {
		return "color";
	}

	@Override
	public Object[] getTranslationParameters() {
		return new Object[] { I18n.get("color.minecraft." + color.getName()) };
	}

	@Override
	public void writeNBT(CompoundTag nbt) {
		nbt.putInt("id", color.getId());
	}

	@Override
	public ItemAttribute readNBT(CompoundTag nbt) {
		return nbt.contains("id") ?
			new ColorAttribute(DyeColor.byId(nbt.getInt("id")))
			: EMPTY;
	}
}
