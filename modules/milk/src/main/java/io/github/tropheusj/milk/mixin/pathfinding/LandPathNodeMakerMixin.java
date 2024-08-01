package io.github.tropheusj.milk.mixin.pathfinding;

import io.github.tropheusj.milk.Milk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WalkNodeEvaluator.class)
public class LandPathNodeMakerMixin {
	@ModifyVariable(method = "getStart()Lnet/minecraft/world/level/pathfinder/Node;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
	private BlockState treatMilkAsWater(BlockState state) {
		if (Milk.isMilk(state)) {
			return Blocks.WATER.defaultBlockState();
		}
		return state;
	}
}
