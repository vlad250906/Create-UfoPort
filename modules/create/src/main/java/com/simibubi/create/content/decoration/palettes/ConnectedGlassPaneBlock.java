package com.simibubi.create.content.decoration.palettes;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class ConnectedGlassPaneBlock extends GlassPaneBlock {

	public ConnectedGlassPaneBlock(Properties builder) {
		super(builder);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
		if (side.getAxis()
			.isVertical())
			return adjacentBlockState == state;
		return super.skipRendering(state, adjacentBlockState, side);
	}

}
