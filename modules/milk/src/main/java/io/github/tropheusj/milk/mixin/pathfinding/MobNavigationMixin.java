package io.github.tropheusj.milk.mixin.pathfinding;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import io.github.tropheusj.milk.Milk;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(GroundPathNavigation.class)
public class MobNavigationMixin {
	@ModifyVariable(
			method = "getSurfaceY",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
			)
	)
	private BlockState treatMilkAsWater(BlockState state) {
		if (Milk.isMilk(state)) {
			return Blocks.WATER.defaultBlockState();
		}
		return state;
	}
}
