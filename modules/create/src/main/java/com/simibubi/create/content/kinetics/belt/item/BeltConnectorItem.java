package com.simibubi.create.content.kinetics.belt.item;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class BeltConnectorItem extends BlockItem {

	public BeltConnectorItem(Properties properties) {
		super(AllBlocks.BELT.get(), properties);
	}

	@Override
	public String getDescriptionId() {
		return getOrCreateDescriptionId();
	}


	@Nonnull
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player playerEntity = context.getPlayer();
		if (playerEntity != null && playerEntity.isShiftKeyDown()) {
			ItemHelper.clearComponents(context.getItemInHand());
			return InteractionResult.SUCCESS;
		}

		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		boolean validAxis = validateAxis(world, pos);

		if (world.isClientSide)
			return validAxis ? InteractionResult.SUCCESS : InteractionResult.FAIL;

		//CompoundTag tag = context.getItemInHand()
				//.getOrCreateTag();
		BlockPos firstPulley = null;

		// Remove first if no longer existant or valid
		if (context.getItemInHand().has(AllDataComponents.FIRST_PULLEY)) {
			firstPulley = context.getItemInHand().get(AllDataComponents.FIRST_PULLEY);
			if (!validateAxis(world, firstPulley) || !firstPulley.closerThan(pos, maxLength() * 2)) {
				context.getItemInHand().remove(AllDataComponents.FIRST_PULLEY);
//				tag.remove("FirstPulley");
//				context.getItemInHand()
//						.setTag(tag);
			}
		}

		if (!validAxis || playerEntity == null)
			return InteractionResult.FAIL;

		if (context.getItemInHand().has(AllDataComponents.FIRST_PULLEY)) {

			if (!canConnect(world, firstPulley, pos))
				return InteractionResult.FAIL;

			if (firstPulley != null && !firstPulley.equals(pos)) {
				createBelts(world, firstPulley, pos);
				AllAdvancements.BELT.awardTo(playerEntity);
				if (!playerEntity.isCreative())
					context.getItemInHand()
							.shrink(1);
			}

			if (!context.getItemInHand()
					.isEmpty()) {
				ItemHelper.clearComponents(context.getItemInHand());
				playerEntity.getCooldowns()
						.addCooldown(this, 5);
			}
			return InteractionResult.SUCCESS;
		}
		
		context.getItemInHand().set(AllDataComponents.FIRST_PULLEY, pos);
		playerEntity.getCooldowns()
				.addCooldown(this, 5);
		return InteractionResult.SUCCESS;
	}

	public static void createBelts(Level world, BlockPos start, BlockPos end) {
		world.playSound(null, BlockPos.containing(VecHelper.getCenterOf(start.offset(end))
				.scale(.5f)), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.5F, 1F);

		BeltSlope slope = getSlopeBetween(start, end);
		Direction facing = getFacingFromTo(start, end);

		BlockPos diff = end.subtract(start);
		if (diff.getX() == diff.getZ())
			facing = Direction.get(facing.getAxisDirection(), world.getBlockState(start)
					.getValue(BlockStateProperties.AXIS) == Axis.X ? Axis.Z : Axis.X);

		List<BlockPos> beltsToCreate = getBeltChainBetween(start, end, slope, facing);
		BlockState beltBlock = AllBlocks.BELT.getDefaultState();
		boolean failed = false;

		for (BlockPos pos : beltsToCreate) {
			BlockState existingBlock = world.getBlockState(pos);
			if (existingBlock.getDestroySpeed(world, pos) == -1) {
				failed = true;
				break;
			}

			BeltPart part = pos.equals(start) ? BeltPart.START : pos.equals(end) ? BeltPart.END : BeltPart.MIDDLE;
			BlockState shaftState = world.getBlockState(pos);
			boolean pulley = ShaftBlock.isShaft(shaftState);
			if (part == BeltPart.MIDDLE && pulley)
				part = BeltPart.PULLEY;
			if (pulley && shaftState.getValue(AbstractSimpleShaftBlock.AXIS) == Axis.Y)
				slope = BeltSlope.SIDEWAYS;

			if (!existingBlock.canBeReplaced())
				world.destroyBlock(pos, false);

			KineticBlockEntity.switchToBlockState(world, pos,
				ProperWaterloggedBlock.withWater(world, beltBlock.setValue(BeltBlock.SLOPE, slope)
					.setValue(BeltBlock.PART, part)
					.setValue(BeltBlock.HORIZONTAL_FACING, facing), pos));
		}

		if (!failed)
			return;

		for (BlockPos pos : beltsToCreate)
			if (AllBlocks.BELT.has(world.getBlockState(pos)))
				world.destroyBlock(pos, false);
	}

	private static Direction getFacingFromTo(BlockPos start, BlockPos end) {
		Axis beltAxis = start.getX() == end.getX() ? Axis.Z : Axis.X;
		BlockPos diff = end.subtract(start);
		AxisDirection axisDirection = AxisDirection.POSITIVE;

		if (diff.getX() == 0 && diff.getZ() == 0)
			axisDirection = diff.getY() > 0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;
		else
			axisDirection =
					beltAxis.choose(diff.getX(), 0, diff.getZ()) > 0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;

		return Direction.get(axisDirection, beltAxis);
	}

	private static BeltSlope getSlopeBetween(BlockPos start, BlockPos end) {
		BlockPos diff = end.subtract(start);

		if (diff.getY() != 0) {
			if (diff.getZ() != 0 || diff.getX() != 0)
				return diff.getY() > 0 ? BeltSlope.UPWARD : BeltSlope.DOWNWARD;
			return BeltSlope.VERTICAL;
		}
		return BeltSlope.HORIZONTAL;
	}

	private static List<BlockPos> getBeltChainBetween(BlockPos start, BlockPos end, BeltSlope slope,
													  Direction direction) {
		List<BlockPos> positions = new LinkedList<>();
		int limit = 1000;
		BlockPos current = start;

		do {
			positions.add(current);

			if (slope == BeltSlope.VERTICAL) {
				current = current.above(direction.getAxisDirection() == AxisDirection.POSITIVE ? 1 : -1);
				continue;
			}

			current = current.relative(direction);
			if (slope != BeltSlope.HORIZONTAL)
				current = current.above(slope == BeltSlope.UPWARD ? 1 : -1);

		} while (!current.equals(end) && limit-- > 0);

		positions.add(end);
		return positions;
	}

	public static boolean canConnect(Level world, BlockPos first, BlockPos second) {
		if (!world.isLoaded(first) || !world.isLoaded(second))
			return false;
		if (!second.closerThan(first, maxLength()))
			return false;

		BlockPos diff = second.subtract(first);
		Axis shaftAxis = world.getBlockState(first)
				.getValue(BlockStateProperties.AXIS);

		int x = diff.getX();
		int y = diff.getY();
		int z = diff.getZ();
		int sames = ((Math.abs(x) == Math.abs(y)) ? 1 : 0) + ((Math.abs(y) == Math.abs(z)) ? 1 : 0)
				+ ((Math.abs(z) == Math.abs(x)) ? 1 : 0);

		if (shaftAxis.choose(x, y, z) != 0)
			return false;
		if (sames != 1)
			return false;
		if (shaftAxis != world.getBlockState(second)
				.getValue(BlockStateProperties.AXIS))
			return false;
		if (shaftAxis == Axis.Y && x != 0 && z != 0)
			return false;

		BlockEntity blockEntity = world.getBlockEntity(first);
		BlockEntity blockEntity2 = world.getBlockEntity(second);

		if (!(blockEntity instanceof KineticBlockEntity))
			return false;
		if (!(blockEntity2 instanceof KineticBlockEntity))
			return false;

		float speed1 = ((KineticBlockEntity) blockEntity).getTheoreticalSpeed();
		float speed2 = ((KineticBlockEntity) blockEntity2).getTheoreticalSpeed();
		if (Math.signum(speed1) != Math.signum(speed2) && speed1 != 0 && speed2 != 0)
			return false;

		BlockPos step = BlockPos.containing(Math.signum(diff.getX()), Math.signum(diff.getY()), Math.signum(diff.getZ()));
		int limit = 1000;
		for (BlockPos currentPos = first.offset(step); !currentPos.equals(second) && limit-- > 0; currentPos =
				currentPos.offset(step)) {
			BlockState blockState = world.getBlockState(currentPos);
			if (ShaftBlock.isShaft(blockState) && blockState.getValue(AbstractSimpleShaftBlock.AXIS) == shaftAxis)
				continue;
			if (!blockState.canBeReplaced())
				return false;
		}

		return true;

	}

	public static Integer maxLength() {
		return AllConfigs.server().kinetics.maxBeltLength.get();
	}

	public static boolean validateAxis(Level world, BlockPos pos) {
		if (!world.isLoaded(pos))
			return false;
		if (!ShaftBlock.isShaft(world.getBlockState(pos)))
			return false;
		return true;
	}

}
