package io.github.tropheusj.dripstone_fluid_lib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.tropheusj.dripstone_fluid_lib.DripstoneInteractingFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;

@Mixin(CauldronBlock.class)
public abstract class CauldronBlockMixin {
	@Inject(method = "receiveStalactiteDrip", at = @At("HEAD"), cancellable = true)
	private void dripstone_fluid_lib$receiveStalactiteDrip(BlockState state, Level world, BlockPos pos, Fluid fluid, CallbackInfo ci) {
		if (fluid instanceof DripstoneInteractingFluid interactingFluid) {
			if (interactingFluid.fillsCauldrons(state, world, pos)) {
				BlockState newState = interactingFluid.getCauldronBlockState(state, world, pos);
				world.setBlockAndUpdate(pos, newState);
				world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
				world.levelEvent(interactingFluid.getFluidDripWorldEvent(state, world, pos), pos, 0);
				ci.cancel();
			}
		}
	}
}
