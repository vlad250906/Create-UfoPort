package com.tterrag.registrate.fabric;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public class FluidHelper {
	public static String getDescriptionId(Fluid fluid) {
		return Util.makeDescriptionId("fluid", BuiltInRegistries.FLUID.getKey(fluid));
	}

	public static int fluidLuminanceFromBlockState(BlockState fluidBlockState) {
		FluidState state = fluidBlockState.getFluidState();
		if (state.isEmpty()) {
			return 0;
		}
		Fluid fluid = state.getType();
		state = fluid.defaultFluidState(); // FluidVariant.of checks against this
		if (!fluid.isSource(state)) {
			if (fluid instanceof FlowingFluid flowing) {
				fluid = flowing.getSource();
				state = fluid.defaultFluidState();
			}
		}
		if (!fluid.isSource(state)) {
			return 0; // checking will crash
		}
		FluidVariant variant = FluidVariant.of(fluid);
		return FluidVariantAttributes.getLuminance(variant);
	}
}
