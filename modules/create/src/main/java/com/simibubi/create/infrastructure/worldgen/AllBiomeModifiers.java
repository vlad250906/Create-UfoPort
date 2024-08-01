package com.simibubi.create.infrastructure.worldgen;

import java.util.function.Predicate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

public class AllBiomeModifiers {
	public static void bootstrap() {
		Predicate<BiomeSelectionContext> isOverworld = BiomeSelectors.foundInOverworld();
		Predicate<BiomeSelectionContext> isNether = BiomeSelectors.foundInTheNether();

		addOre(isOverworld, AllPlacedFeatures.ZINC_ORE);
		addOre(isOverworld, AllPlacedFeatures.STRIATED_ORES_OVERWORLD);
		addOre(isNether, AllPlacedFeatures.STRIATED_ORES_NETHER);
	}

	private static void addOre(Predicate<BiomeSelectionContext> test, ResourceKey<PlacedFeature> feature) {
		BiomeModifications.addFeature(test, Decoration.UNDERGROUND_ORES, feature);
	}
}
