package com.simibubi.create.content.kinetics.transmission;

import com.simibubi.create.content.kinetics.base.DirectionalShaftHalvesBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class SplitShaftBlockEntity extends DirectionalShaftHalvesBlockEntity {

	public SplitShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract float getRotationSpeedModifier(Direction face);

}
