package io.github.tropheusj.milk.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import io.github.tropheusj.milk.Milk;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;

import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@ModifyVariable(
			method = "updateFluidHeightAndDoFluidPushing",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lnet/minecraft/entity/Entity;getWorld()Lnet/minecraft/world/level/Level;",
							ordinal = 0
					)
			),
			at = @At(value = "STORE"
			)
	)
	public FluidState milk$clearEffectsInMilk(FluidState state) {
		if ((Object) this instanceof LivingEntity entity && Milk.STILL_MILK != null) {
			if (Milk.isMilk(state)) {
				if (entity.getActiveEffects().size() > 0) {
					entity.removeAllEffects();
				}
			}
		}
		return state;
	}
}
