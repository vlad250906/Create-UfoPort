package com.simibubi.create.content.decoration.palettes;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class WindowBlock extends ConnectedGlassBlock {

	protected final boolean translucent;

	public WindowBlock(Properties p_i48392_1_, boolean translucent) {
		super(p_i48392_1_);
		this.translucent = translucent;
	}

	public boolean isTranslucent() {
		return translucent;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
		if (state.getBlock() == adjacentBlockState.getBlock()) {
			return true;
		}
		if (state.getBlock() instanceof WindowBlock windowBlock
				&& adjacentBlockState.getBlock() instanceof ConnectedGlassBlock) {
			return !windowBlock.isTranslucent() && side.getAxis().isHorizontal();
		}
		return super.skipRendering(state, adjacentBlockState, side);
	}

}
