package com.simibubi.create.content.kinetics.simpleRelays.encased;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;

import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class EncasedShaftBlock extends AbstractEncasedShaftBlock
	implements IBE<KineticBlockEntity>, ISpecialBlockItemRequirement, BlockPickInteractionAware, EncasedBlock {

	private final Supplier<Block> casing;

	public EncasedShaftBlock(Properties properties, Supplier<Block> casing) {
		super(properties);
		this.casing = casing;
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		if (context.getLevel().isClientSide)
			return InteractionResult.SUCCESS;
		context.getLevel()
			.levelEvent(2001, context.getClickedPos(), Block.getId(state));
		KineticBlockEntity.switchToBlockState(context.getLevel(), context.getClickedPos(),
			AllBlocks.SHAFT.getDefaultState()
				.setValue(AXIS, state.getValue(AXIS)));
		return InteractionResult.SUCCESS;
	}

	@Override
	public ItemStack getPickedStack(BlockState state, BlockGetter view, BlockPos pos, @Nullable Player player, @Nullable HitResult target) {
		if (target instanceof BlockHitResult)
			return ((BlockHitResult) target).getDirection()
				.getAxis() == getRotationAxis(state) ? AllBlocks.SHAFT.asStack() : getCasing().asItem().getDefaultInstance();
		return ItemStack.EMPTY;
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		return ItemRequirement.of(AllBlocks.SHAFT.getDefaultState(), be);
	}

	@Override
	public Class<KineticBlockEntity> getBlockEntityClass() {
		return KineticBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ENCASED_SHAFT.get();
	}

	@Override
	public Block getCasing() {
		return casing.get();
	}

	@Override
	public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player, InteractionHand hand,
	    BlockHitResult ray) {
		KineticBlockEntity.switchToBlockState(level, pos, defaultBlockState()
				.setValue(RotatedPillarKineticBlock.AXIS, state.getValue(RotatedPillarKineticBlock.AXIS)));
	}
}
