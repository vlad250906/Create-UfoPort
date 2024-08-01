package com.simibubi.create.content.equipment.bell;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllPartialModels;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PeculiarBellBlockEntity extends AbstractBellBlockEntity {

	public PeculiarBellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public PartialModel getBellModel() {
		return AllPartialModels.PECULIAR_BELL;
	}

}
