package com.simibubi.create.content.fluids.potion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllFluids;
import com.simibubi.create.content.fluids.VirtualFluid;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

public class PotionFluid extends VirtualFluid {

	public PotionFluid(Properties properties) {
		super(properties);
	}

	public static FluidStack of(long amount, Holder<Potion> potion) {
		FluidStack fluidStack = new FluidStack(AllFluids.POTION.get()
				.getSource(), amount);
		return addPotionToFluidStack(fluidStack, potion);
	}

	public static FluidStack withEffects(long amount, Holder<Potion> potion, List<MobEffectInstance> customEffects) {
		FluidStack fluidStack = of(amount, potion);
		return appendEffects(fluidStack, customEffects);
	}

	public static FluidStack addPotionToFluidStack(FluidStack fs, Holder<Potion> potion) {
		if (potion == null) {
			fs.remove(DataComponents.POTION_CONTENTS);
			return new FluidStack(fs.getFluid(), fs.getAmount(), (PatchedDataComponentMap)fs.getComponents());
		}
		
		ResourceLocation resourcelocation = RegisteredObjects.getKeyOrThrow(potion.value());
		fs.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
		return new FluidStack(fs.getFluid(), fs.getAmount(), (PatchedDataComponentMap)fs.getComponents());
	}

	public static FluidStack appendEffects(FluidStack fs, Collection<MobEffectInstance> customEffects) {
		if (customEffects.isEmpty())
			return fs;
		
		PotionContents potions = fs.getOrCreateComponent(DataComponents.POTION_CONTENTS, 
				new PotionContents(Optional.empty(), Optional.empty(), new ArrayList()));
		for (MobEffectInstance effectinstance : customEffects)
			potions.withEffectAdded(effectinstance);
		return new FluidStack(fs.getFluid(), fs.getAmount(), (PatchedDataComponentMap)fs.getComponents());
	}

	public enum BottleType {
		REGULAR, SPLASH, LINGERING;
	}
/*
	// fabric: PotionFluidVariantRenderHandler and PotionFluidVariantAttributeHandler in AllFluids
	public static class PotionFluidAttributes extends FluidAttributes {

		public PotionFluidAttributes(Builder builder, Fluid fluid) {
			super(builder, fluid);
		}

		@Override
		public int getColor(FluidStack stack) {
			CompoundTag tag = stack.getOrCreateTag();
			int color = PotionUtils.getColor(PotionUtils.getAllEffects(tag)) | 0xff000000;
			return color;
		}

		@Override
		public Component getDisplayName(FluidStack stack) {
			return Components.translatable(getTranslationKey(stack));
		}

		@Override
		public String getTranslationKey(FluidStack stack) {
			CompoundTag tag = stack.getOrCreateTag();
			ItemLike itemFromBottleType =
					PotionFluidHandler.itemFromBottleType(NBTHelper.readEnum(tag, "Bottle", BottleType.class));
			return PotionUtils.getPotion(tag)
					.getName(itemFromBottleType.asItem()
							.getDescriptionId() + ".effect.");
		}

	}*/

}
