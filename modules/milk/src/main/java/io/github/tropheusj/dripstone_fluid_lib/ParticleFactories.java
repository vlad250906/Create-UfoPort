package io.github.tropheusj.dripstone_fluid_lib;

import io.github.tropheusj.dripstone_fluid_lib.mixin.WaterSplashParticle$SplashFactoryAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SplashParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.material.Fluid;

@Environment(EnvType.CLIENT)
public class ParticleFactories {
	/**
	 * {@link BlockLeakParticle#createDrippingDripstoneWater(DefaultParticleType, ClientWorld, double, double, double, double, double, double)}
	 */
	public record DrippingDripstoneFluidFactory(DripstoneInteractingFluid fluid)
			implements ParticleProvider.Sprite<ParticleOptions> {
		@Override
		public TextureSheetParticle createParticle(ParticleOptions defaultParticleType, ClientLevel clientWorld,
													  double x, double y, double z,
													  double velocityX, double velocityY, double velocityZ) {
			DripParticle particle = new DripParticle.DripHangParticle(
					clientWorld, x, y, z, (Fluid) fluid, Constants.FLUIDS_TO_PARTICLES.get(fluid).fall()
			);
			int color = fluid.getParticleColor(clientWorld, x, y, z, velocityX, velocityY, velocityZ);
			float r = (color >> 16 & 255) / 255f;
			float g = (color >> 8 & 255) / 255f;
			float b = (color & 255) / 255f;
			particle.setColor(r, g, b);
			return particle;
		}
	}

	public record FallingDripstoneFluidFactory(DripstoneInteractingFluid fluid)
			implements ParticleProvider.Sprite<ParticleOptions> {
		@Override
		public TextureSheetParticle createParticle(ParticleOptions defaultParticleType, ClientLevel clientWorld,
													  double x, double y, double z,
													  double velocityX, double velocityY, double velocityZ) {
			DripParticle particle = new DripParticle.DripstoneFallAndLandParticle(
					clientWorld, x, y, z, (Fluid) fluid, Constants.FLUIDS_TO_PARTICLES.get(fluid).splash()
			);
			int color = fluid.getParticleColor(clientWorld, x, y, z, velocityX, velocityY, velocityZ);
			float r = (color >> 16 & 255) / 255f;
			float g = (color >> 8 & 255) / 255f;
			float b = (color & 255) / 255f;
			particle.setColor(r, g, b);
			return particle;
		}
	}

	public static class DripstoneFluidSplashFactory extends SplashParticle.Provider {
		private final DripstoneInteractingFluid fluid;

		public DripstoneFluidSplashFactory(SpriteSet spriteProvider, DripstoneInteractingFluid fluid) {
			super(spriteProvider);
			this.fluid = fluid;
		}

		@Override
		public Particle createParticle(SimpleParticleType defaultParticleType, ClientLevel clientWorld,
									   double x, double y, double z,
									   double velocityX, double velocityY, double velocityZ) {
			SplashParticle particle = new DripstoneFluidParticle(clientWorld, x, y, z, velocityX, velocityY, velocityZ);
			particle.setSprite((TextureAtlasSprite) ((WaterSplashParticle$SplashFactoryAccessor) this).dripstone_fluid_lib$SpriteSet());
			int color = fluid.getParticleColor(clientWorld, x, y, z, velocityX, velocityY, velocityZ);
			float r = (color >> 16 & 255) / 255f;
			float g = (color >> 8 & 255) / 255f;
			float b = (color & 255) / 255f;
			particle.setColor(r, g, b);
			return particle;
		}
	}
}
