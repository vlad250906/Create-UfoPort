package io.github.tropheusj.dripstone_fluid_lib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus.Internal;

import com.google.common.collect.ImmutableList;

import net.minecraft.resources.ResourceLocation;

public class Constants {
	public static final List<ResourceLocation> DRIP_HANG = ImmutableList.of(ResourceLocation.fromNamespaceAndPath("minecraft", "drip_hang"));
	public static final List<ResourceLocation> DRIP_FALL = ImmutableList.of(ResourceLocation.fromNamespaceAndPath("minecraft", "drip_fall"));
	public static final List<ResourceLocation> SPLASH = ImmutableList.of(
			ResourceLocation.fromNamespaceAndPath("dripstone_fluid_lib", "splash_0"),
			ResourceLocation.fromNamespaceAndPath("dripstone_fluid_lib", "splash_1"),
			ResourceLocation.fromNamespaceAndPath("dripstone_fluid_lib", "splash_2"),
			ResourceLocation.fromNamespaceAndPath("dripstone_fluid_lib", "splash_3")
	);

	public static final Map<DripstoneInteractingFluid, ParticleTypeSet> FLUIDS_TO_PARTICLES = new HashMap<>();
	@Internal
	public static final Set<DripstoneInteractingFluid> TO_REGISTER = new HashSet<>();
}
