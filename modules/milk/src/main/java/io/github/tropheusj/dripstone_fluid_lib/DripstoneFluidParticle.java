package io.github.tropheusj.dripstone_fluid_lib;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SplashParticle;

@Environment(EnvType.CLIENT)
public class DripstoneFluidParticle extends SplashParticle {
	public DripstoneFluidParticle(ClientLevel clientWorld, double d, double e, double f, double g, double h, double i) {
		super(clientWorld, d, e, f, g, h, i);
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if (this.age-- <= 0) {
			this.remove();
		} else {
			this.yd -= this.gravity;
			this.move(this.xd, this.yd, this.zd);
			this.xd *= 0.98F;
			this.yd *= 0.98F;
			this.zd *= 0.98F;
			if (this.onGround) {
				if (Math.random() < 0.5) {
					this.remove();
					;
				}

				this.xd *= 0.7F;
				this.yd *= 0.7F;
			}
		}
	}
}
