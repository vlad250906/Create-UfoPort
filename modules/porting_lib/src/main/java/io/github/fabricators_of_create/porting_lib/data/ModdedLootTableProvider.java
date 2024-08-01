package io.github.fabricators_of_create.porting_lib.data;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class ModdedLootTableProvider extends LootTableProvider {
	private static final Logger LOGGER = LogUtils.getLogger();
	private final Set<ResourceKey<LootTable>> requiredTables;
	private final List<LootTableProvider.SubProviderEntry> subProviders;
	private CompletableFuture<HolderLookup.Provider> registries;

	public ModdedLootTableProvider(PackOutput packOutput, Set<ResourceKey<LootTable>> requiredTables,
			List<SubProviderEntry> subProviders, CompletableFuture<HolderLookup.Provider> registries) {
		super(packOutput, requiredTables, subProviders, registries);
		this.requiredTables = requiredTables;
		this.subProviders = subProviders;
		this.registries = registries;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput pOutput) {
		return this.registries.thenCompose(provider -> this.run(pOutput, (HolderLookup.Provider)provider));
	}
	
	public CompletableFuture<?> run(CachedOutput pOutput, HolderLookup.Provider provider) {
		final Map<ResourceKey<LootTable>, LootTable> map = Maps.newHashMap();
		Map<RandomSupport.Seed128bit, ResourceKey<LootTable>> map1 = new Object2ObjectOpenHashMap<>();
		this.getTables().forEach((entry) -> entry.provider().apply(provider).generate((key, builder) -> {
			ResourceKey<LootTable> id = map1.put(RandomSequence.seedForKey(key.location()), key);
			if (id != null) {
				Util.logAndPauseIfInIde("Loot table random sequence seed collision on " + id + " and " + key);
			}

			builder.setRandomSequence(key.location());
			if (map.put(key, builder.setParamSet(entry.paramSet()).build()) != null) {
				throw new IllegalStateException("Duplicate loot table " + key);
			}
		}));
		ProblemReporter.Collector prc = new ProblemReporter.Collector();
		ValidationContext validationcontext = new ValidationContext(prc, LootContextParamSets.ALL_PARAMS,
				/*new HolderGetter.Provider() {
					@Nullable
					public <T> T getElement(LootDataId<T> p_279283_) {
						return (T) (p_279283_.type() == LootDataType.TABLE ? map.get(p_279283_.location()) : null);
					}
				}*/ provider.asGetterLookup());

		validate(map, validationcontext);

		Multimap<String, String> multimap = prc.get();
		if (!multimap.isEmpty()) {
			multimap.forEach((p_124446_, p_124447_) -> {
				LOGGER.warn("Found validation problem in {}: {}", p_124446_, p_124447_);
			});
			throw new IllegalStateException("Failed to validate loot tables, see logs");
		} else {
			return CompletableFuture.allOf(map.entrySet().stream().map((lootTableEntry) -> {
				ResourceKey<LootTable> lootTableId = lootTableEntry.getKey();
				LootTable loottable = lootTableEntry.getValue();
				Path path = this.pathProvider.json(lootTableId.location());
				JsonElement je = LootTable./*CODEC*/DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, loottable).result().get();
				return DataProvider.saveStable(pOutput, je, path);
			}).toArray(CompletableFuture[]::new));
		}
	}

	public List<LootTableProvider.SubProviderEntry> getTables() {
		return this.subProviders;
	}

	protected void validate(Map<ResourceKey<LootTable>, LootTable> map, ValidationContext validationcontext) {
		for (ResourceKey<LootTable> resourcelocation : Sets.difference(this.requiredTables, map.keySet())) {
			validationcontext.reportProblem("Missing built-in table: " + resourcelocation);
		}

		map.forEach((id, lootTable) -> {
			lootTable.validate(validationcontext.setParams(lootTable.getParamSet()).enterElement("{" + id + "}", id));
		});
	}

	public String getName() {
		return "LootTables";
	}
}
