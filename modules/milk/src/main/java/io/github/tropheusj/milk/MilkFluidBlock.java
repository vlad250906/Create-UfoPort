package io.github.tropheusj.milk;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

public class MilkFluidBlock extends LiquidBlock {
	protected MilkFluidBlock(FlowingFluid fluid, BlockBehaviour.Properties settings) {
		super(fluid, settings);
	}
}
