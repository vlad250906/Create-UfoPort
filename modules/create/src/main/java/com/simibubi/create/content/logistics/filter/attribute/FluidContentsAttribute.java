package com.simibubi.create.content.logistics.filter.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.simibubi.create.content.logistics.filter.ItemAttribute;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public class FluidContentsAttribute implements ItemAttribute {
	public static final FluidContentsAttribute EMPTY = new FluidContentsAttribute(null);

	private final Fluid fluid;

	public FluidContentsAttribute(@Nullable Fluid fluid) {
		this.fluid = fluid;
	}

	@Override
	public boolean appliesTo(ItemStack itemStack) {
		return extractFluids(itemStack).contains(fluid);
	}

	@Override
	public List<ItemAttribute> listAttributesOf(ItemStack itemStack) {
		return extractFluids(itemStack).stream().map(FluidContentsAttribute::new).collect(Collectors.toList());
	}

	@Override
	public String getTranslationKey() {
		return "has_fluid";
	}

	@Override
	public Object[] getTranslationParameters() {
		String parameter = "";
		if (fluid != null)
			parameter = FluidVariantAttributes.getName(FluidVariant.of(fluid)).getString();
		return new Object[] { parameter };
	}

	@Override
	public void writeNBT(CompoundTag nbt) {
		if (fluid == null)
			return;
		ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
		if (id == null)
			return;
		nbt.putString("id", id.toString());
	}

	@Override
	public ItemAttribute readNBT(CompoundTag nbt) {
		return nbt.contains("id")
				? new FluidContentsAttribute(
						BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(nbt.getString("id"))))
				: EMPTY;
	}

	private List<Fluid> extractFluids(ItemStack stack) {
		List<Fluid> fluids = new ArrayList<>();

//        LazyOptional<IFluidHandlerItem> capability =
//                stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
//
//        capability.ifPresent((cap) -> {
//            for(int i = 0; i < cap.getTanks(); i++) {
//                fluids.add(cap.getFluidInTank(i).getFluid());
//            }
//        });

		return fluids;
	}
}
