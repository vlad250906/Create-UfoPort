package com.simibubi.create.content.equipment.wrench;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags.AllItemTags;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class WrenchEventHandler {
	public static InteractionResult useOwnWrenchLogicForCreateBlocks(Player player, Level world, InteractionHand hand, BlockHitResult hitVec) {
		ItemStack itemStack = player.getItemInHand(hand);

		// fabric: note - mayBuild handles spectator check
		if (!player.mayBuild())
			return InteractionResult.PASS;
		if (itemStack.isEmpty())
			return InteractionResult.PASS;
		if (AllItems.WRENCH.isIn(itemStack))
			return InteractionResult.PASS;
		if (!AllItemTags.WRENCH.matches(itemStack.getItem()))
			return InteractionResult.PASS;

		BlockState state = world
			.getBlockState(hitVec.getBlockPos());
		Block block = state.getBlock();

		if (!(block instanceof IWrenchable))
			return InteractionResult.PASS;

		UseOnContext context = new UseOnContext(player, hand, hitVec);
		IWrenchable actor = (IWrenchable) block;

		return player.isShiftKeyDown() ? actor.onSneakWrenched(state, context) : actor.onWrenched(state, context);
	}

}
