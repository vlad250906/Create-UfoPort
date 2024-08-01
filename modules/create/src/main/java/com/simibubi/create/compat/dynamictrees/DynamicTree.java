package com.simibubi.create.compat.dynamictrees;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.utility.AbstractBlockBreakQueue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

// Commented until dynamic trees are updated
public class DynamicTree extends AbstractBlockBreakQueue {
	
//	private BlockPos startCutPos;

	public DynamicTree(BlockPos startCutPos) {
//		this.startCutPos = startCutPos;
	}
	
	public static boolean isDynamicBranch(Block block) {
		return false; // TreeHelper.isBranch(block) || block instanceof TrunkShellBlock;
	}

	
	@Override
	public void destroyBlocks(Level world, ItemStack toDamage, @Nullable Player playerEntity, BiConsumer<BlockPos, ItemStack> drop) {
		/*
		
		BranchBlock start = TreeHelper.getBranch(world.getBlockState(startCutPos));
		if (start == null) //if start is null, it was not a branch
			start = setBranchToShellMuse(world, world.getBlockState(startCutPos)); //we check for a trunk shell instead

		if (start == null) //if it is null again, it was neither a branch nor a trunk shell and thus we return
			return;

		// Play and render block break sound and particles
		world.levelEvent(null, 2001, startCutPos, Block.getId(world.getBlockState(startCutPos)));
		// Actually breaks the tree

		BranchDestructionData data = start.destroyBranchFromNode(world, startCutPos, Direction.DOWN, false, playerEntity);

		// Feed all the tree drops to drop bi-consumer
		data.leavesDrops.forEach(stackPos -> drop.accept(stackPos.pos.offset(startCutPos), stackPos.stack));
		start.getLogDrops(world, startCutPos, data.species, data.woodVolume).forEach(stack -> drop.accept(startCutPos, stack));
		
		*/
	}

	/*
	private BranchBlock setBranchToShellMuse(World world, BlockState state) {
		 
		Block block = state.getBlock();
		if (block instanceof TrunkShellBlock){
			TrunkShellBlock.ShellMuse muse = ((TrunkShellBlock)block).getMuse(world, startCutPos);
			if (muse != null){
				startCutPos = muse.pos; //the cut pos is moved to the center of the trunk
				return TreeHelper.getBranch(muse.state);
			}
		}
		
		return null;
	}
	*/
	

}
