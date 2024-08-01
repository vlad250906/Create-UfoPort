package com.simibubi.create.foundation.data;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.fabric.SimpleFlowableFluid;
import com.tterrag.registrate.fabric.SimpleFlowableFluid.Properties;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.resources.ResourceLocation;

/**
 * For registering fluids with no buckets/blocks
 */
public class VirtualFluidBuilder<T extends SimpleFlowableFluid, P> extends FluidBuilder<T, P> {

	public VirtualFluidBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback,
		ResourceLocation stillTexture, ResourceLocation flowingTexture,
//		FluidBuilder.FluidTypeFactory typeFactory,
		NonNullFunction<Properties, T> factory) {
		super(owner, parent, name, callback, stillTexture, flowingTexture, /*typeFactory, */factory);
		source(factory);
	}

	@Override
	public NonNullSupplier<T> asSupplier() {
		return this::getEntry;
	}

}
