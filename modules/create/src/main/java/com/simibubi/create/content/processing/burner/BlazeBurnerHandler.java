package com.simibubi.create.content.processing.burner;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity.FuelType;

import io.github.fabricators_of_create.porting_lib.entity.events.ProjectileImpactEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BlazeBurnerHandler {

	public static boolean onThrowableImpact(ProjectileImpactEvent event) {
		splashExtinguishesBurner(event.getProjectile(), event.getRayTraceResult());
		return thrownEggsGetEatenByBurner(event.getProjectile(), event.getRayTraceResult());
	}

	public static boolean thrownEggsGetEatenByBurner(Projectile projectile, HitResult hitResult) {
//		Projectile projectile = event.getProjectile();
		if (!(projectile instanceof ThrownEgg))
			return false;

		if (hitResult.getType() != HitResult.Type.BLOCK)
			return false;

		BlockEntity blockEntity = projectile.level().getBlockEntity(BlockPos.containing(hitResult.getLocation()));
		if (!(blockEntity instanceof BlazeBurnerBlockEntity)) {
			return false;
		}

//		event.setCanceled(true);
		projectile.setDeltaMovement(Vec3.ZERO);
		projectile.discard();

		Level world = projectile.level();
		if (world.isClientSide)
			return false;

		BlazeBurnerBlockEntity heater = (BlazeBurnerBlockEntity) blockEntity;
		if (!heater.isCreative()) {
			if (heater.activeFuel != FuelType.SPECIAL) {
				heater.activeFuel = FuelType.NORMAL;
				heater.remainingBurnTime = Mth.clamp(heater.remainingBurnTime + 80, 0,
						BlazeBurnerBlockEntity.MAX_HEAT_CAPACITY);
				heater.updateBlockState();
				heater.notifyUpdate();
			}
		}

		AllSoundEvents.BLAZE_MUNCH.playOnServer(world, heater.getBlockPos());
		return true;
	}

	public static void splashExtinguishesBurner(Projectile projectile, HitResult hitResult) {
//		Projectile projectile = event.getProjectile();
		if (projectile.level().isClientSide)
			return;
		if (!(projectile instanceof ThrownPotion))
			return;
		ThrownPotion entity = (ThrownPotion) projectile;

		if (hitResult.getType() != HitResult.Type.BLOCK)
			return;

		ItemStack stack = entity.getItem();
		Potion potion = stack.has(DataComponents.POTION_CONTENTS) ? 
				stack.get(DataComponents.POTION_CONTENTS).potion().isPresent() ? 
						stack.get(DataComponents.POTION_CONTENTS).potion().get().value() 
						: null : null;
		if (potion == Potions.WATER && stack.get(DataComponents.POTION_CONTENTS).customEffects().isEmpty()) {
			BlockHitResult result = (BlockHitResult) hitResult;
			Level world = entity.level();
			Direction face = result.getDirection();
			BlockPos pos = result.getBlockPos().relative(face);

			extinguishLitBurners(world, pos, face);
			extinguishLitBurners(world, pos.relative(face.getOpposite()), face);

			for (Direction face1 : Direction.Plane.HORIZONTAL) {
				extinguishLitBurners(world, pos.relative(face1), face1);
			}
		}
	}

	private static void extinguishLitBurners(Level world, BlockPos pos, Direction direction) {
		BlockState state = world.getBlockState(pos);
		if (AllBlocks.LIT_BLAZE_BURNER.has(state)) {
			world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
					2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
			world.setBlockAndUpdate(pos, AllBlocks.BLAZE_BURNER.getDefaultState());
		}
	}

}
