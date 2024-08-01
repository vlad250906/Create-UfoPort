package io.github.fabricators_of_create.porting_lib.data;

import java.util.Set;
import java.util.function.BiConsumer;

import com.google.common.collect.Sets;

import io.github.fabricators_of_create.porting_lib.PortingLibBase;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTable.Builder;

public abstract class ModdedBlockLootSubProvider extends BlockLootSubProvider {
	protected ModdedBlockLootSubProvider(Set<Item> set, FeatureFlagSet featureFlagSet, HolderLookup.Provider prov) {
		super(set, featureFlagSet, prov);
	}
	

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> biConsumer) {
		this.generate();
		Set<ResourceLocation> set = Sets.<ResourceLocation>newHashSet();

		for(Block block : getKnownBlocks()) {
			ResourceKey<LootTable> resourceLocation = block.getLootTable();
			if (resourceLocation != BuiltInLootTables.EMPTY && set.add(resourceLocation.location())) {
				LootTable.Builder builder6 = map.remove(resourceLocation);
				if (builder6 == null) {
					throw new IllegalStateException(String.format("Missing loottable '%s' for '%s'", resourceLocation, BuiltInRegistries.BLOCK.getKey(block)));
				}

				biConsumer.accept(resourceLocation, builder6);
			}
		}

		if (!map.isEmpty()) {
			throw new IllegalStateException("Created block loot tables for non-blocks: " + map.keySet());
		}
	}

	protected Iterable<Block> getKnownBlocks() {
		return BuiltInRegistries.BLOCK;
	}
}
