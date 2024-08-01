package com.simibubi.create.content.kinetics.crank;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.Material;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class HandCrankBlockEntity extends GeneratingKineticBlockEntity {

	public int inUse;
	public boolean backwards;
	public float independentAngle;
	public float chasingVelocity;

	public HandCrankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public void turn(boolean back) {
		boolean update = false;

		if (getGeneratedSpeed() == 0 || back != backwards)
			update = true;

		inUse = 10;
		this.backwards = back;
		if (update && !level.isClientSide)
			updateGeneratedRotation();
	}

	public float getIndependentAngle(float partialTicks) {
		return (independentAngle + partialTicks * chasingVelocity) / 360;
	}

	@Override
	public float getGeneratedSpeed() {
		Block block = getBlockState().getBlock();
		if (!(block instanceof HandCrankBlock))
			return 0;
		HandCrankBlock crank = (HandCrankBlock) block;
		int speed = (inUse == 0 ? 0 : clockwise() ? -1 : 1) * crank.getRotationSpeed();
		return convertToDirection(speed, getBlockState().getValue(HandCrankBlock.FACING));
	}

	protected boolean clockwise() {
		return backwards;
	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		compound.putInt("InUse", inUse);
		compound.putBoolean("Backwards", backwards);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, Provider reg, boolean clientPacket) {
		inUse = compound.getInt("InUse");
		backwards = compound.getBoolean("Backwards");
		super.read(compound, reg, clientPacket);
	}

	@Override
	public void tick() {
		super.tick();

		float actualSpeed = getSpeed();
		chasingVelocity += ((actualSpeed * 10 / 3f) - chasingVelocity) * .25f;
		independentAngle += chasingVelocity;

		if (inUse > 0) {
			inUse--;

			if (inUse == 0 && !level.isClientSide) {
				sequenceContext = null;
				updateGeneratedRotation();
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public SuperByteBuffer getRenderedHandle() {
		BlockState blockState = getBlockState();
		Direction facing = blockState.getOptionalValue(HandCrankBlock.FACING).orElse(Direction.UP);
		return CachedBufferer.partialFacing(AllPartialModels.HAND_CRANK_HANDLE, blockState, facing.getOpposite());
	}

	@Environment(EnvType.CLIENT)
	public Instancer<ModelData> getRenderedHandleInstance(Material<ModelData> material) {
		BlockState blockState = getBlockState();
		Direction facing = blockState.getOptionalValue(HandCrankBlock.FACING).orElse(Direction.UP);
		return material.getModel(AllPartialModels.HAND_CRANK_HANDLE, blockState, facing.getOpposite());
	}

	@Environment(EnvType.CLIENT)
	public boolean shouldRenderShaft() {
		return true;
	}

	@Override
	protected Block getStressConfigKey() {
		return AllBlocks.HAND_CRANK.has(getBlockState()) ? AllBlocks.HAND_CRANK.get()
				: AllBlocks.COPPER_VALVE_HANDLE.get();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void tickAudio() {
		super.tickAudio();
		if (inUse > 0 && AnimationTickHolder.getTicks() % 10 == 0) {
			if (!AllBlocks.HAND_CRANK.has(getBlockState()))
				return;
			AllSoundEvents.CRANKING.playAt(level, worldPosition, (inUse) / 2.5f, .65f + (10 - inUse) / 10f, true);
		}
	}

}
