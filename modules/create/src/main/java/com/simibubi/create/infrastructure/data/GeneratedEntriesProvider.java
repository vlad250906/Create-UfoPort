package com.simibubi.create.infrastructure.data;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.worldgen.AllBiomeModifiers;
import com.simibubi.create.infrastructure.worldgen.AllConfiguredFeatures;
import com.simibubi.create.infrastructure.worldgen.AllPlacedFeatures;


import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.RegistrySetBuilder.RegistryBootstrap;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class GeneratedEntriesProvider extends RegistriesDatapackGenerator {

	public static final RegistrySetBuilder BUILDER = Util.make(new RegistrySetBuilder(), GeneratedEntriesProvider::addBootstraps);

	public GeneratedEntriesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries /*, BUILDER, Set.of(Create.ID)*/);
	}

	// fabric: this must be reused in the entrypoint, moved to a method
	public static void addBootstraps(RegistrySetBuilder builder) {
		builder.add(Registries.DAMAGE_TYPE, AllDamageTypes::bootstrap)
				.add(Registries.CONFIGURED_FEATURE, btsr -> AllConfiguredFeatures.bootstrap(btsr))
				.add(Registries.PLACED_FEATURE, AllPlacedFeatures::bootstrap);
		// fabric: biome modifiers not a registry, remove
	}

	@Override
	public String getName() {
		return "Create's Generated Registry Entries";
	}
}
