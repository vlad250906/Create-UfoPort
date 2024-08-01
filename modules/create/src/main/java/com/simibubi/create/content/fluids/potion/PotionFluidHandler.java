package com.simibubi.create.content.fluids.potion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.Pair;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;

public class PotionFluidHandler {

	public static Pair<FluidStack, ItemStack> emptyPotion(ItemStack stack, boolean simulate) {
		FluidStack fluid = getFluidFromPotionItem(stack);
		if (!simulate)
			stack.shrink(1);
		return Pair.of(fluid, new ItemStack(Items.GLASS_BOTTLE));
	}

	public static FluidIngredient potionIngredient(Holder<Potion> potion, long amount) {
		ItemStack is = new ItemStack(Items.POTION);
		is.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
		return FluidIngredient.fromFluidStack(FluidHelper.copyStackWithAmount(PotionFluidHandler
			.getFluidFromPotionItem(is), amount));
	}

	public static FluidStack getFluidFromPotionItem(ItemStack stack) {
		Holder<Potion> potion = stack.has(DataComponents.POTION_CONTENTS) ? 
				(stack.get(DataComponents.POTION_CONTENTS).potion().isPresent() ? 
						stack.get(DataComponents.POTION_CONTENTS).potion().get() 
						: null) : null;
		if (potion == null)
			return FluidStack.EMPTY;
		
		List<MobEffectInstance> list = stack.get(DataComponents.POTION_CONTENTS).customEffects();
		BottleType bottleTypeFromItem = bottleTypeFromItem(stack.getItem());
		
		if (potion == Potions.WATER && list.isEmpty() && bottleTypeFromItem == BottleType.REGULAR)
			return new FluidStack(Fluids.WATER, FluidConstants.BOTTLE);
		
		FluidStack fluid = PotionFluid.withEffects(FluidConstants.BOTTLE, potion, list);
		CompoundTag tagInfo = fluid.getOrCreateComponent(AllDataComponents.BOTTLE_TYPE, new CompoundTag());
		NBTHelper.writeEnum(tagInfo, "Bottle", bottleTypeFromItem);
		FluidVariant variant = FluidVariant.of(fluid.getFluid(), ((PatchedDataComponentMap)fluid.getComponents()).asPatch());
		return new FluidStack(variant, fluid.getAmount(), (PatchedDataComponentMap)fluid.getComponents());
	}

	public static FluidStack getFluidFromPotion(Holder<Potion> potion, BottleType bottleType, long amount) {
		if (potion == Potions.WATER && bottleType == BottleType.REGULAR)
			return new FluidStack(Fluids.WATER, amount);
		FluidStack fluid = PotionFluid.of(amount, potion);
		NBTHelper.writeEnum(fluid.getOrCreateComponent(AllDataComponents.BOTTLE_TYPE, new CompoundTag()), "Bottle", bottleType);
		return new FluidStack(fluid.getFluid(), fluid.getAmount(), (PatchedDataComponentMap)fluid.getComponents());
	}

	public static BottleType bottleTypeFromItem(Item item) {
		if (item == Items.LINGERING_POTION)
			return BottleType.LINGERING;
		if (item == Items.SPLASH_POTION)
			return BottleType.SPLASH;
		return BottleType.REGULAR;
	}

	public static ItemLike itemFromBottleType(BottleType type) {
		switch (type) {
		case LINGERING:
			return Items.LINGERING_POTION;
		case SPLASH:
			return Items.SPLASH_POTION;
		case REGULAR:
		default:
			return Items.POTION;
		}
	}

	public static long getRequiredAmountForFilledBottle(ItemStack stack, FluidStack availableFluid) {
		return FluidConstants.BOTTLE;
	}

	public static ItemStack fillBottle(ItemStack stack, FluidStack availableFluid) {
		CompoundTag tag = availableFluid.getOrDefault(AllDataComponents.BOTTLE_TYPE, new CompoundTag());
		ItemStack potionStack = new ItemStack(itemFromBottleType(NBTHelper.readEnum(tag, "Bottle", BottleType.class)));
		potionStack.set(DataComponents.POTION_CONTENTS, availableFluid.get(DataComponents.POTION_CONTENTS));
		//PotionUtils.setPotion(potionStack, PotionUtils.getPotion(tag));
		//PotionUtils.setCustomEffects(potionStack, PotionUtils.getCustomEffects(tag));
		return potionStack;
	}

	// Modified version of PotionUtils#addPotionTooltip
	@Environment(EnvType.CLIENT)
	public static void addPotionTooltip(FluidStack fs, List<Component> tooltip, float p_185182_2_) {
		addPotionTooltip(fs.getType(), tooltip, p_185182_2_);
	}

	@Environment(EnvType.CLIENT)
	public static void addPotionTooltip(FluidVariant fs, List<Component> tooltip, float p_185182_2_) {
		Iterable<MobEffectInstance> iterable = fs.getComponents().get(DataComponents.POTION_CONTENTS) == null 
				|| fs.getComponents().get(DataComponents.POTION_CONTENTS).isEmpty() 
				? List.of() : fs.getComponents().get(DataComponents.POTION_CONTENTS).get().getAllEffects();
		List<MobEffectInstance> list = new ArrayList<MobEffectInstance>();
		Iterator<MobEffectInstance> iter = iterable.iterator();
		while(iter.hasNext()) {
			list.add(iter.next());
		}
		
		List<Tuple<String, AttributeModifier>> list1 = Lists.newArrayList();
		if (list.isEmpty()) {
			tooltip.add((Components.translatable("effect.none")).withStyle(ChatFormatting.GRAY));
		} else {
			for (MobEffectInstance effectinstance : list) {
				MutableComponent textcomponent = Components.translatable(effectinstance.getDescriptionId());
				MobEffect effect = effectinstance.getEffect().value();
				Map<Holder<Attribute>, MobEffect.AttributeTemplate> map = effect.attributeModifiers;
				if (!map.isEmpty()) {
					for (Entry<Holder<Attribute>, MobEffect.AttributeTemplate> entry : map.entrySet()) {
						AttributeModifier attributemodifier = entry.getValue().create(effectinstance.getAmplifier());
						
						AttributeModifier attributemodifier1 = new AttributeModifier(
								ResourceLocation.fromNamespaceAndPath("create", attributemodifier.operation().getSerializedName()),
								attributemodifier.amount(),
							attributemodifier.operation());
						list1.add(new Tuple<>(
							entry.getKey().value().getDescriptionId(),
							attributemodifier1));
					}
				}

				if (effectinstance.getAmplifier() > 0) {
					textcomponent.append(" ")
						.append(Components.translatable("potion.potency." + effectinstance.getAmplifier()).getString());
				}

				if (effectinstance.getDuration() > 20) {
					textcomponent.append(" (")
						.append(MobEffectUtil.formatDuration(effectinstance, p_185182_2_, 1))
						.append(")");
				}

				tooltip.add(textcomponent.withStyle(effect.getCategory()
					.getTooltipFormatting()));
			}
		}

		if (!list1.isEmpty()) {
			tooltip.add(Components.immutableEmpty());
			tooltip.add((Components.translatable("potion.whenDrank")).withStyle(ChatFormatting.DARK_PURPLE));

			for (Tuple<String, AttributeModifier> tuple : list1) {
				AttributeModifier attributemodifier2 = tuple.getB();
				double d0 = attributemodifier2.amount();
				double d1;
				if (attributemodifier2.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE
					&& attributemodifier2.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
					d1 = attributemodifier2.amount();
				} else {
					d1 = attributemodifier2.amount() * 100.0D;
				}

				if (d0 > 0.0D) {
					tooltip.add((Components.translatable(
						"attribute.modifier.plus." + attributemodifier2.operation()
							.id(),
							ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d1),
						Components.translatable(tuple.getA())))
							.withStyle(ChatFormatting.BLUE));
				} else if (d0 < 0.0D) {
					d1 = d1 * -1.0D;
					tooltip.add((Components.translatable(
						"attribute.modifier.take." + attributemodifier2.operation()
							.id(),
						ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d1),
						Components.translatable(tuple.getA())))
							.withStyle(ChatFormatting.RED));
				}
			}
		}

	}

}
