package com.simibubi.create.content.fluids;

import com.tterrag.registrate.fabric.SimpleFlowableFluid;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public class VirtualFluid extends SimpleFlowableFluid {

	public VirtualFluid(Properties properties) {
		super(properties);
	}

	@Override
	public Fluid getSource() {
		return super.getSource();
	}

	@Override
	public Fluid getFlowing() {
		return this;
	}

	@Override
	public Item getBucket() {
		return Items.AIR;
	}

	@Override
	protected BlockState createLegacyBlock(FluidState state) {
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public boolean isSource(FluidState p_207193_1_) {
		return true;
	}

	@Override
	public int getAmount(FluidState p_207192_1_) {
		return 0;
	}

}
