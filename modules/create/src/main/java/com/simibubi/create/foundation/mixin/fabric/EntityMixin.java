package com.simibubi.create.foundation.mixin.fabric;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.foundation.ponder.PonderWorld;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.phys.shapes.VoxelShape;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.EntityAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Shadow
	public abstract Level level();

	// AbstractMinecart does not override remove, so we have to inject here.
	@Inject(method = "remove", at = @At("HEAD"))
	private void removeMinecartController(RemovalReason reason, CallbackInfo ci) {
		//noinspection ConstantValue
		if ((Object) this instanceof AbstractMinecart cart) {
			CapabilityMinecartController.onCartRemoved(level(), cart);
		}
	}

	/**
	 * @author AeiouEnigma
	 * @reason We stan Lithium's collision optimizations but need to ensure they aren't applied in Create's PonderWorld.
	 */
	@Inject(method = "collideBoundingBox", at = @At("HEAD"), cancellable = true)
	private static void create$stopLithiumCollisionChangesInPonderWorld(@Nullable Entity entity, Vec3 movement, AABB entityBoundingBox, Level world, List<VoxelShape> shapes, CallbackInfoReturnable<Vec3> ci) {
		if (world instanceof PonderWorld) {
			// Vanilla copy
			ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(shapes.size() + 1);
			if (!shapes.isEmpty()) {
				builder.addAll(shapes);
			}

			WorldBorder worldBorder = world.getWorldBorder();
			boolean bl = entity != null && worldBorder.isInsideCloseToBorder(entity, entityBoundingBox.expandTowards(movement));
			if (bl) {
				builder.add(worldBorder.getCollisionShape());
			}

			builder.addAll(world.getBlockCollisions(entity, entityBoundingBox.expandTowards(movement)));
			// Prevent Lithium's changes from executing for PonderWorlds
			ci.setReturnValue(EntityAccessor.port_lib$collideWithShapes(movement, entityBoundingBox, builder.build()));

		}
	}
}
