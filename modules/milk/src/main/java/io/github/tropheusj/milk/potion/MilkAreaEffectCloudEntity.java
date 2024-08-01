package io.github.tropheusj.milk.potion;

import java.util.List;
import java.util.Optional;

import io.github.tropheusj.milk.Milk;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

public class MilkAreaEffectCloudEntity extends AreaEffectCloud {
	public MilkAreaEffectCloudEntity(Level world, double d, double e, double f) {
		super(world, d, e, f);
		super.setPotionContents(new PotionContents(Optional.empty(), Optional.of(0xFFFFFF), List.of()));
	}

	@Override
	public void tick() {
		boolean bl = this.isWaiting();
		float f = this.getRadius();
		if (this.level().isClientSide()) {
			if (bl && this.random.nextBoolean()) {
				return;
			}
			
			ParticleOptions particleEffect = this.getParticle();
			int i;
			float g;
			if (bl) {
				i = 2;
				g = 0.2F;
			} else {
				i = Mth.ceil((float) Math.PI * f * f);
				g = f;
			}

			for(int k = 0; k < i; ++k) {
				float l = this.random.nextFloat() * (float) (Math.PI * 2);
				float m = Mth.sqrt(this.random.nextFloat()) * g;
				double d = this.getX() + (double)(Mth.cos(l) * m);
				double e = this.getY();
				double n = this.getZ() + (double)(Mth.sin(l) * m);
				double s;
				double t;
				double u;
				if (particleEffect.getType() != ParticleTypes.ENTITY_EFFECT) {
					if (bl) {
						s = 0.0;
						t = 0.0;
						u = 0.0;
					} else {
						s = (0.5 - this.random.nextDouble()) * 0.15;
						t = 0.01F;
						u = (0.5 - this.random.nextDouble()) * 0.15;
					}
				} else {
					int o = bl && this.random.nextBoolean() ? 16777215 : 0xFFFFFF;
					s = ((float)(o >> 16 & 0xFF) / 255.0F);
					t = ((float)(o >> 8 & 0xFF) / 255.0F);
					u = ((float)(o & 0xFF) / 255.0F);
				}

				this.level().addParticle(particleEffect, d, e, n, s, t, u);
			}
		} else {
			if (this.tickCount >= getWaitTime() + getDuration()) {
				this.discard();
				return;
			}

			boolean bl2 = this.tickCount < getWaitTime();
			if (bl != bl2) {
				this.setWaiting(bl2);
			}

			if (bl2) {
				return;
			}

			if (getRadiusPerTick() != 0.0F) {
				f += getRadiusPerTick();
				if (f < 0.5F) {
					this.discard();
					return;
				}

				this.setRadius(f);
			}

			if (this.tickCount % 5 == 0) {
				level().getEntities(this, getBoundingBox().inflate(2)).forEach(entity -> {
					if (entity instanceof LivingEntity livingEntity) {
						Milk.tryRemoveRandomEffect(livingEntity);
					}
				});
			}
		}
	}
}
