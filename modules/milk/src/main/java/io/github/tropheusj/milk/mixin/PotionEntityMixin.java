package io.github.tropheusj.milk.mixin;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.datafixers.optics.Lens.Box;

import io.github.tropheusj.milk.Milk;
import io.github.tropheusj.milk.potion.MilkAreaEffectCloudEntity;
import io.github.tropheusj.milk.potion.bottle.LingeringMilkBottle;
import io.github.tropheusj.milk.potion.bottle.PotionItemEntityExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@Mixin(ThrownPotion.class)
public abstract class PotionEntityMixin extends ThrowableItemProjectile implements ItemSupplier, PotionItemEntityExtensions {
	@Shadow
	protected abstract void applySplash(Iterable<MobEffectInstance> statusEffects, @Nullable Entity entity);

	@Shadow protected abstract void makeAreaOfEffectCloud(PotionContents potionContents);

	@Shadow protected abstract void dowseFire(BlockPos pos);

	@Shadow
	protected abstract void applyWater();

	@Unique
	private boolean milk = false;

	public PotionEntityMixin(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
		super(entityType, world);
	}

	@Inject(method = "onHitBlock", at = @At(value = "HEAD"))
	protected void milk$onHitBlock(BlockHitResult blockHitResult, CallbackInfo ci) {
		if (isMilk()) {
			Direction side = blockHitResult.getDirection();
			BlockPos pos = blockHitResult.getBlockPos().offset(side.getNormal());
			this.dowseFire(pos);
			this.dowseFire(pos.offset(side.getOpposite().getNormal()));

			for(Direction direction2 : Direction.Plane.HORIZONTAL) {
				this.dowseFire(pos.offset(direction2.getNormal()));
			}
		}
	}

	@Inject(method = "onHit", at = @At(value = "HEAD"), cancellable = true)
	protected void milk$onHit(HitResult hitResult, CallbackInfo ci) {
		if (isMilk()) {
			super.onHit(hitResult);
			if (!this.level().isClientSide()) {
				applyWater();
				if (getItem().getItem() instanceof LingeringMilkBottle) {
					makeAreaOfEffectCloud(new PotionContents(Optional.empty(), Optional.empty(), List.of()));
				} else {
					applySplash(null, hitResult.getType() == HitResult.Type.ENTITY ? ((EntityHitResult) hitResult).getEntity() : null);
				}
				this.level().levelEvent(LevelEvent.PARTICLES_INSTANT_POTION_SPLASH, this.getOnPos(), 0xFFFFFF);
				this.discard();
			}
			ci.cancel();
		}
	}

	@Inject(method = "applySplash", at = @At("HEAD"), cancellable = true)
	private void milk$applySplash(Iterable<MobEffectInstance> statusEffects, Entity entity, CallbackInfo ci) {
		if (isMilk()) {
			AABB box = this.getBoundingBox().expandTowards(4.0, 2.0, 4.0);
			List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, box);
			if (!list.isEmpty()) {
				for (LivingEntity livingEntity : list) {
					if (livingEntity.isAffectedByPotions()) {
						double d = this.distanceTo(livingEntity);
						if (d < 16.0) {
							Milk.tryRemoveRandomEffect(livingEntity);
						}
					}
				}
			}
			ci.cancel();
		}
	}

	@Inject(method = "makeAreaOfEffectCloud", at = @At("HEAD"), cancellable = true)
	private void milk$makeAreaOfEffectCloud(PotionContents conts, CallbackInfo ci) {
		if (isMilk()) {
			MilkAreaEffectCloudEntity areaEffectCloudEntity = new MilkAreaEffectCloudEntity(this.level(), this.getX(), this.getY(), this.getZ());
			Entity entity = this.getOwner();
			if (entity instanceof LivingEntity) {
				areaEffectCloudEntity.setOwner((LivingEntity) entity);
			}

			areaEffectCloudEntity.setRadius(3.0F);
			areaEffectCloudEntity.setRadiusOnUse(-0.5F);
			areaEffectCloudEntity.setWaitTime(10);
			areaEffectCloudEntity.setRadius(-areaEffectCloudEntity.getRadius() / (float) areaEffectCloudEntity.getDuration());

			this.level().addFreshEntity(areaEffectCloudEntity);
			ci.cancel();
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.putBoolean("Milk", milk);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		setMilk(nbt.getBoolean("Milk"));
	}

	@Override
	public boolean isMilk() {
		return milk;
	}

	@Override
	public void setMilk(boolean value) {
		milk = value;
	}
}
