package io.github.fabricators_of_create.porting_lib.tool.extensions;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

// TODO: Re-evaluate how to make this work
@Deprecated(forRemoval = true)
public interface BlockExtensions {
	/**
	 * Returns the state that this block should transform into when right-clicked by
	 * a tool. For example: Used to determine if {@link ToolActions#AXE_STRIP an axe
	 * can strip}, {@link ToolActions#SHOVEL_FLATTEN a shovel can path}, or
	 * {@link ToolActions#HOE_TILL a hoe can till}. Returns {@code null} if nothing
	 * should happen.
	 *
	 * @param state      The current state
	 * @param context    The use on context that the action was performed in
	 * @param toolAction The action being performed by the tool
	 * @param simulate   If {@code true}, no actions that modify the world in any
	 *                   way should be performed. If {@code false}, the world may be
	 *                   modified.
	 * @return The resulting state after the action has been performed
	 */
	@Nullable
	default BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction toolAction,
			boolean simulate) {
		BlockState toolModifiedState = getToolModifiedState(state, context.getLevel(), context.getClickedPos(),
				context.getPlayer(), context.getItemInHand(), toolAction);

		if (toolModifiedState == null && ToolActions.HOE_TILL == toolAction
				&& context.getItemInHand().canPerformAction(ToolActions.HOE_TILL)) {
			// Logic copied from HoeItem#TILLABLES; needs to be kept in sync during updating
			Block block = state.getBlock();
			if (block == Blocks.ROOTED_DIRT) {
				if (!simulate && !context.getLevel().isClientSide) {
					Block.popResourceFromFace(context.getLevel(), context.getClickedPos(), context.getClickedFace(),
							new ItemStack(Items.HANGING_ROOTS));
				}
				return Blocks.DIRT.defaultBlockState();
			} else if ((block == Blocks.GRASS_BLOCK || block == Blocks.DIRT_PATH || block == Blocks.DIRT
					|| block == Blocks.COARSE_DIRT)
					&& context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
				return block == Blocks.COARSE_DIRT ? Blocks.DIRT.defaultBlockState()
						: Blocks.FARMLAND.defaultBlockState();
			}
		}

		return toolModifiedState;
	}

	/**
	 * Returns the state that this block should transform into when right clicked by
	 * a tool. For example: Used to determine if an axe can strip, a shovel can
	 * path, or a hoe can till. Return null if vanilla behavior should be disabled.
	 *
	 * @param state      The current state
	 * @param world      The world
	 * @param pos        The block position in world
	 * @param player     The player clicking the block
	 * @param stack      The stack being used by the player
	 * @param toolAction The action being performed by the tool
	 * @return The resulting state after the action has been performed
	 */
	@Nullable
	default BlockState getToolModifiedState(BlockState state, Level world, BlockPos pos, Player player, ItemStack stack,
			ToolAction toolAction) {
		if (!stack.canPerformAction(toolAction))
			return null;
		if (ToolActions.AXE_STRIP.equals(toolAction)) {
			Block block = AxeItem.STRIPPABLES.get(state.getBlock());
			return block != null ? block.defaultBlockState() : null;
		} else if (ToolActions.AXE_SCRAPE.equals(toolAction))
			return WeatheringCopper.getPrevious(state).orElse(null);
		else if (ToolActions.AXE_WAX_OFF.equals(toolAction))
			return Optional.ofNullable(HoneycombItem.WAX_OFF_BY_BLOCK.get().get(state.getBlock())).map((p_150694_) -> {
				return p_150694_.withPropertiesOf(state);
			}).orElse(null);
		// else if(ToolActions.HOE_TILL.equals(toolAction)) return
		// HoeItem.getHoeTillingState(state); //TODO HoeItem bork
		else if (ToolActions.SHOVEL_FLATTEN.equals(toolAction))
			return ShovelItem.FLATTENABLES.get(state.getBlock());
		return null;
	}
}
