package com.simibubi.create.content.decoration;

import java.util.function.Predicate;

import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MetalLadderBlock extends LadderBlock implements IWrenchable {

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public MetalLadderBlock(Properties p_54345_) {
		super(p_54345_);
	}

//	@Override // fabric: difficult to implement with little to gain
//	@Environment(EnvType.CLIENT)
//	public boolean supportsExternalFaceHiding(BlockState state) {
//		return false;
//	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pDirection) {
		return pDirection == Direction.UP && pAdjacentBlockState.getBlock() instanceof LadderBlock;
	}
	
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult ray) {
		
		if (player.isShiftKeyDown() || !player.mayBuild())
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		ItemStack heldItem = player.getItemInHand(hand);
		if(heldItem.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		
		IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
		if (helper.matchesItem(heldItem))
			return helper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);
		return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		
	}
	
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
			BlockHitResult ray) {
		
		if (player.isShiftKeyDown() || !player.mayBuild())
			return InteractionResult.PASS;
		ItemStack heldItem = ItemStack.EMPTY;
		IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
		if (helper.matchesItem(heldItem))
			return helper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, InteractionHand.MAIN_HAND, ray).result();
		return InteractionResult.PASS;
		
	}
	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof MetalLadderBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return s -> s.getBlock() instanceof LadderBlock;
		}

		public int attachedLadders(Level world, BlockPos pos, Direction direction) {
			BlockPos checkPos = pos.relative(direction);
			BlockState state = world.getBlockState(checkPos);
			int count = 0;
			while (getStatePredicate().test(state)) {
				count++;
				checkPos = checkPos.relative(direction);
				state = world.getBlockState(checkPos);
			}
			return count;
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			Direction dir = player.getXRot() < 0 ? Direction.UP : Direction.DOWN;

			int range = AllConfigs.server().equipment.placementAssistRange.get();
			if (player != null) {
				AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
				if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier.id()))
					range += 4;
			}

			int ladders = attachedLadders(world, pos, dir);
			if (ladders >= range)
				return PlacementOffset.fail();

			BlockPos newPos = pos.relative(dir, ladders + 1);
			BlockState newState = world.getBlockState(newPos);

			if (!state.canSurvive(world, newPos))
				return PlacementOffset.fail();

			if (newState.canBeReplaced())
				return PlacementOffset.success(newPos, bState -> bState.setValue(FACING, state.getValue(FACING)));
			return PlacementOffset.fail();
		}

	}

}
