package io.github.fabricators_of_create.porting_lib.tool.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import io.github.fabricators_of_create.porting_lib.tool.loot.CanToolPerformAction;
import io.github.fabricators_of_create.porting_lib.tool.mixin.BuilderAccessor;
import io.github.fabricators_of_create.porting_lib.tool.mixin.InvertedLootItemConditionAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.Util;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

/**
 * Currently used only for replacing shears item to shears_dig tool action
 */
public final class ToolActionsLootTableProvider extends LootTableProvider {
	
	private final CompletableFuture<HolderLookup.Provider> registries;
	
	public ToolActionsLootTableProvider(FabricDataOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
		super(packOutput, Set.of(), VanillaLootTableProvider.create(packOutput, registries).subProviders, registries);
		this.registries = registries;
	}

	protected void validate(Map<ResourceKey<LootTable>, LootTable> map, ValidationContext validationcontext) {
		// Do not validate against all registered loot tables
	}

	public List<LootTableProvider.SubProviderEntry> getTables() {
		return this.subProviders.stream().map(entry -> {
			// Provides new sub provider with filtering only changed loot tables and replacing condition item to condition tag
			return new LootTableProvider.SubProviderEntry((prov) -> replaceAndFilterChangesOnly(entry.provider().apply(prov)), entry.paramSet());
		}).collect(Collectors.toList());
	}

	private LootTableSubProvider replaceAndFilterChangesOnly(LootTableSubProvider subProvider) {
		return (bicons) -> subProvider.generate((resourceLocation, builder) -> {
			if (findAndReplaceInLootTableBuilder(builder, Items.SHEARS, ToolActions.SHEARS_DIG)) {
				bicons.accept(resourceLocation, builder);
			}
		});
	}

	private boolean findAndReplaceInLootTableBuilder(LootTable.Builder builder, Item from, ToolAction toolAction) {
		Builder<LootPool> lootPools = ((BuilderAccessor) builder).getPools();
		boolean found = false;

		for (LootPool lootPool : lootPools.build()) {
			if (findAndReplaceInLootPool(lootPool, from, toolAction)) {
				found = true;
			}
		}

		return found;
	}

	private boolean findAndReplaceInLootPool(LootPool lootPool, Item from, ToolAction toolAction) {
		List<LootPoolEntryContainer> lootEntriesWas = lootPool.entries;
		if (lootEntriesWas == null) {
			throw new IllegalStateException(LootPool.class.getName() + " is missing field f_7902" + "3_");
		}
		
		List<LootPoolEntryContainer> lootEntries = new ArrayList<LootPoolEntryContainer>(lootEntriesWas);
		lootPool.entries = lootEntries;
		boolean found = false;

		for (LootPoolEntryContainer lootEntry : lootEntries) {
			if (findAndReplaceInLootEntry(lootEntry, from, toolAction)) {
				found = true;
			}
			if (lootEntry instanceof CompositeEntryBase) {
				if (findAndReplaceInParentedLootEntry((CompositeEntryBase) lootEntry, from, toolAction)) {
					found = true;
				}
			}
		}
		
		List<LootItemCondition> lootConditionsWas = lootPool.conditions;
		if (lootConditionsWas == null) {
			throw new IllegalStateException(LootPool.class.getName() + " is missing field f_7902" + "4_");
		}
		
		List<LootItemCondition> lootConditions = new ArrayList<LootItemCondition>(lootConditionsWas);
		lootPool.conditions = lootConditions;

		for (int i = 0; i < lootConditions.size(); i++) {
			LootItemCondition lootCondition = lootConditions.get(i);
			if (lootCondition instanceof MatchTool && checkMatchTool((MatchTool) lootCondition, from)) {
				lootConditions.set(i, CanToolPerformAction.canToolPerformAction(toolAction).build());
				found = true;
			} else if (lootCondition instanceof InvertedLootItemCondition) {
				LootItemCondition invLootCondition = ((InvertedLootItemConditionAccessor) lootCondition).getTerm();

				if (invLootCondition instanceof MatchTool && checkMatchTool((MatchTool) invLootCondition, from)) {
					lootConditions.set(i, InvertedLootItemCondition.invert(CanToolPerformAction.canToolPerformAction(toolAction)).build());
					found = true;
				} else if (invLootCondition instanceof CompositeLootItemCondition compositeLootItemCondition && findAndReplaceInComposite(compositeLootItemCondition, from, toolAction)) {
					found = true;
				}
			}
		}

		return found;
	}

	private boolean findAndReplaceInParentedLootEntry(CompositeEntryBase entry, Item from, ToolAction toolAction) {
		List<LootPoolEntryContainer> lootEntries = entry.children;
		boolean found = false;

		if (lootEntries == null) {
			throw new IllegalStateException(CompositeEntryBase.class.getName() + " is missing field f_7942" + "8_");
		}

		for (LootPoolEntryContainer lootEntry : lootEntries) {
			if (findAndReplaceInLootEntry(lootEntry, from, toolAction)) {
				found = true;
			}
		}

		return found;
	}

	private boolean findAndReplaceInLootEntry(LootPoolEntryContainer entry, Item from, ToolAction toolAction) {
		List<LootItemCondition> lootConditionsWas = entry.conditions;
		if (lootConditionsWas == null) {
			throw new IllegalStateException(CompositeLootItemCondition.class.getName() + " is missing field f_28560" + "9_");
		}
		
		List<LootItemCondition> lootConditions = new ArrayList<LootItemCondition>(lootConditionsWas);
		entry.conditions = lootConditions;
		boolean found = false;

		for (int i = 0; i < lootConditions.size(); i++) {
			if (lootConditions.get(i) instanceof CompositeLootItemCondition composite && findAndReplaceInComposite(composite, from, toolAction)) {
				found = true;
			} else if (lootConditions.get(i) instanceof MatchTool && checkMatchTool((MatchTool) lootConditions.get(i), from)) {
				lootConditions.set(i, CanToolPerformAction.canToolPerformAction(toolAction).build());
				found = true;
			}
		}

		return found;
	}

	private boolean findAndReplaceInComposite(CompositeLootItemCondition alternative, Item from, ToolAction toolAction) {
		List<LootItemCondition> lootConditionsWas = alternative.terms;
		if (lootConditionsWas == null) {
			throw new IllegalStateException(CompositeLootItemCondition.class.getName() + " is missing field f_28560" + "9_");
		}
		
		List<LootItemCondition> lootConditionsNew = new ArrayList<LootItemCondition>(lootConditionsWas);
		alternative.terms = lootConditionsNew;
		boolean found = false;

		for (int i = 0; i < lootConditionsNew.size(); i++) {
			if (lootConditionsNew.get(i) instanceof MatchTool && checkMatchTool((MatchTool) lootConditionsNew.get(i), from)) {
				lootConditionsNew.set(i, CanToolPerformAction.canToolPerformAction(toolAction).build());
				found = true;
			}
		}

		return found;
	}

	private boolean checkMatchTool(MatchTool lootCondition, Item expected) {
		return lootCondition
				.predicate()
				.flatMap(ItemPredicate::items)
				.filter(holders -> holders.contains(expected.builtInRegistryHolder()))
				.isPresent();
	}
	
	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		return this.registries.thenCompose(provider -> this.run(output, (HolderLookup.Provider)provider));
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
		ProblemReporter.Collector problemCollector = new ProblemReporter.Collector();
		ValidationContext validationcontext = new ValidationContext(problemCollector, LootContextParamSets.ALL_PARAMS, /*new LootDataResolver() {
			@Nullable
			public <T> T getElement(LootDataId<T> p_279283_) {
				return (T) (p_279283_.type() == LootDataType.TABLE ? map.get(p_279283_.location()) : null);
			}
		}*/ provider.asGetterLookup());

		validate(map, validationcontext);

		Multimap<String, String> multimap = problemCollector.get();
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
				//JsonElement elem = LootTable./*CODEC*/DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, loottable).getOrThrow();
				//return DataProvider.saveStable(pOutput, elem, path);
				return DataProvider.saveStable(pOutput, provider, LootTable.DIRECT_CODEC, loottable, path);
			}).toArray(CompletableFuture[]::new));
		}
	}
}
