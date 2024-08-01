package io.github.tropheusj.dripstone_fluid_lib.mixin;

import io.github.tropheusj.dripstone_fluid_lib.Constants;
import io.github.tropheusj.dripstone_fluid_lib.ParticleTypeSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.tropheusj.dripstone_fluid_lib.DripstoneInteractingFluid;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

@Mixin(Registry.class)
public interface RegistryMixin {
	@Inject(at = @At("HEAD"), method = "register(Lnet/minecraft/core/Registry;Lnet/minecraft/resources/ResourceLocation;Ljava/lang/Object;)Ljava/lang/Object;")
	private static <V, T extends V> void dripstone_fluid_lib$register(Registry<V> registry, ResourceLocation id, T entry, CallbackInfoReturnable<T> cir) {
		if (entry instanceof DripstoneInteractingFluid interactingFluid) {
			SimpleParticleType hang = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
					ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_dripstone_lib_particle_type_hang"),
					FabricParticleTypes.simple());
			SimpleParticleType fall = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
					ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_dripstone_lib_particle_type_fall"),
					FabricParticleTypes.simple());
			SimpleParticleType splash = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
					ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_dripstone_lib_particle_type_splash"),
					FabricParticleTypes.simple());
			Constants.FLUIDS_TO_PARTICLES.put(interactingFluid, new ParticleTypeSet(hang, fall, splash));
		}
	}
}
