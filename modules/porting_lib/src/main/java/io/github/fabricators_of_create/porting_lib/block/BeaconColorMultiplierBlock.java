package io.github.fabricators_of_create.porting_lib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public interface BeaconColorMultiplierBlock {
	/**
	 * @param state The state
	 * @param level The level
	 * @param pos The position of this state
	 * @param beaconPos The position of the beacon
	 * @return A float RGB [0.0, 1.0] array to be averaged with a beacon's existing beam color, or null to do nothing to the beam
	 */
	@Nullable
	default int getBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos) {
		if (this instanceof BeaconBeamBlock beamBlock) {
			int color = beamBlock.getColor().getTextureDiffuseColor();
			float[] arr = {ARGB32.red(color), ARGB32.green(color), ARGB32.blue(color)};
			return color;
		}
		return 0;
	}
}
