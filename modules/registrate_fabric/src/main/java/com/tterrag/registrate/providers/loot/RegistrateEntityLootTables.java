package com.tterrag.registrate.providers.loot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.tterrag.registrate.AbstractRegistrate;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.fabricmc.fabric.impl.biome.modification.BuiltInRegistryKeys;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTable.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class RegistrateEntityLootTables extends SimpleFabricLootTableProvider implements RegistrateLootTables {

	private final AbstractRegistrate<?> parent;
	private final Consumer<RegistrateEntityLootTables> callback;

	private final Map<ResourceKey<LootTable>, Builder> entries = new HashMap<>();

	public RegistrateEntityLootTables(AbstractRegistrate<?> parent, Consumer<RegistrateEntityLootTables> callback,
			FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
		super(output, registryLookup, LootContextParamSets.ENTITY);
		this.parent = parent;
		this.callback = callback;
	}

	@Override
	public void generate(@NotNull BiConsumer<ResourceKey<LootTable>, Builder> consumer) {
		callback.accept(this);
		entries.forEach(consumer);
	}

	public void add(EntityType<?> type, LootTable.Builder table) {
		entries.put(type.getDefaultLootTable(), table);
	}

	public void add(ResourceLocation id, LootTable.Builder table) {
		entries.put(ResourceKey.create(Registries.LOOT_TABLE, id), table);
	}

}
