package com.simibubi.create.compat.jei;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.foundation.utility.NBTHelper;

import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

/* From JEI's Potion item subtype interpreter */
public class PotionFluidSubtypeInterpreter implements IIngredientSubtypeInterpreter<IJeiFluidIngredient> {

	@Override
	public String apply(IJeiFluidIngredient ingredient, UidContext context) {
		if (ingredient.getFluidVariant().getComponents().get(DataComponents.POTION_CONTENTS) == null)
			return IIngredientSubtypeInterpreter.NONE;
		if (ingredient.getFluidVariant().getComponents().get(DataComponents.POTION_CONTENTS).isEmpty())
			return IIngredientSubtypeInterpreter.NONE;

		PotionContents cont = ingredient.getFluidVariant().getComponents().get(DataComponents.POTION_CONTENTS).get();
		Holder<Potion> potionType = cont.potion().isPresent() ? cont.potion().get() : null;
		String potionTypeString = Potion.getName(Optional.ofNullable(potionType), "");
		CompoundTag bottleTag = ingredient.getFluidVariant().getComponents().get(AllDataComponents.BOTTLE_TYPE).isEmpty() 
				? new CompoundTag() : ingredient.getFluidVariant().getComponents().get(AllDataComponents.BOTTLE_TYPE).get();
		String bottleType = NBTHelper.readEnum(bottleTag, "Bottle", BottleType.class)
				.toString();

		StringBuilder stringBuilder = new StringBuilder(potionTypeString);
		List<MobEffectInstance> effects = cont.customEffects();

		stringBuilder.append(";")
				.append(bottleType);
		for (MobEffectInstance effect : potionType.value().getEffects())
			stringBuilder.append(";")
					.append(effect);
		for (MobEffectInstance effect : effects)
			stringBuilder.append(";")
					.append(effect);
		return stringBuilder.toString();
	}

}
