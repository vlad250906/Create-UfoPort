package com.simibubi.create.compat.archEx;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

import com.simibubi.create.content.decoration.palettes.AllPaletteStoneTypes;

import com.simibubi.create.content.decoration.palettes.PaletteBlockPattern;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;

import org.jetbrains.annotations.NotNull;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

public class GroupProvider implements DataProvider {
	public static final Set<PaletteBlockPattern> EXTENDABLE_PATTERNS = Set.of(
			PaletteBlockPattern.CUT, PaletteBlockPattern.POLISHED, PaletteBlockPattern.BRICKS, PaletteBlockPattern.SMALL_BRICKS
	);

	private final FabricDataOutput out;
	public final List<ArchExGroup> groups;

	public GroupProvider(FabricDataOutput out) {
		this.out = out;
		this.groups = generateGroups();
	}


	@Override
	@NotNull
	public CompletableFuture<?> run(@NotNull CachedOutput output) {
		Path outputDir = out.getOutputFolder()
				.resolve("staticdata")
				.resolve("architecture_extensions");
		List<CompletableFuture<?>> saveFutures = new ArrayList<>();

		this.groups.forEach(group -> {
			JsonObject json = group.toJson();
			Path outputFile = outputDir.resolve(group.name() + ".json");
			CompletableFuture<?> future = DataProvider.saveStable(output, json, outputFile);
			saveFutures.add(future);
		});

		return CompletableFuture.allOf(saveFutures.toArray(CompletableFuture[]::new));
	}

	private List<ArchExGroup> generateGroups() {
		List<ArchExGroup> groups = new ArrayList<>();
		for (AllPaletteStoneTypes stoneType : AllPaletteStoneTypes.values()) {
			for (PaletteBlockPattern blockPattern : stoneType.variantTypes) {
				if (EXTENDABLE_PATTERNS.contains(blockPattern)) {
					ArchExGroup group = ArchExGroup.builder()
							.fromStoneTypeAndPattern(stoneType, blockPattern)
							.build();
					groups.add(group);
				}
			}
		}
		return groups;
	}

	@Override
	public @NotNull String getName() {
		return "Create's ArchEx compat";
	}
}
