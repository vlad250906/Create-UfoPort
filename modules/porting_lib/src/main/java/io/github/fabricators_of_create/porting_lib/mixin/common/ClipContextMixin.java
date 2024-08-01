package io.github.fabricators_of_create.porting_lib.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.CollisionContext;

@Mixin(ClipContext.class)
public abstract class ClipContextMixin {
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/CollisionContext;of(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/shapes/CollisionContext;"))
	private static CollisionContext port_lib$redirectCollisionContext(Entity entity) {
		if (entity == null) {
			return CollisionContext.empty();
		}
		return CollisionContext.of(entity);
	}
}
