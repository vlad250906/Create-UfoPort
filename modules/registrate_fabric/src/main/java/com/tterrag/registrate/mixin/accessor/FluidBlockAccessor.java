package com.tterrag.registrate.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

@Mixin(LiquidBlock.class)
public interface FluidBlockAccessor {
	@Invoker("<init>")
	static LiquidBlock callInit(FlowingFluid fluid, BlockBehaviour.Properties settings) {
		throw new AssertionError();
	}
}
