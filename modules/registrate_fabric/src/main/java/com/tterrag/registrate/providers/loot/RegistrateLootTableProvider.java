package com.tterrag.registrate.providers.loot;

import com.google.common.collect.*;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.fabric.CustomValidationLootProvider;
import com.tterrag.registrate.fabric.NonNullQuaFunction;
import com.tterrag.registrate.fabric.NonNullTriFunction;
import com.tterrag.registrate.mixin.accessor.LootContextParamSetsAccessor;
import com.tterrag.registrate.mixin.accessor.LootTableProviderAccessor;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateProvider;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RegistrateLootTableProvider extends LootTableProvider implements RegistrateProvider, CustomValidationLootProvider {

    public interface LootType<T extends RegistrateLootTables> {

        static LootType<RegistrateBlockLootTables> BLOCK = register("block", LootContextParamSets.BLOCK, RegistrateBlockLootTables::new);
        static LootType<RegistrateEntityLootTables> ENTITY = register("entity", LootContextParamSets.ENTITY, RegistrateEntityLootTables::new);

        T getLootCreator(AbstractRegistrate<?> parent, Consumer<T> callback, FabricDataOutput output, CompletableFuture<HolderLookup.Provider> future);
        LootContextParamSet getLootSet();

        static <T extends RegistrateLootTables> LootType<T> register(String name, LootContextParamSet set, 
        		NonNullQuaFunction<AbstractRegistrate, Consumer<T>, FabricDataOutput, CompletableFuture<HolderLookup.Provider>, T> factory) {
            LootType<T> type = new LootType<T>() {
                @Override
                public T getLootCreator(AbstractRegistrate<?> parent, Consumer<T> callback, FabricDataOutput output, CompletableFuture<HolderLookup.Provider> future) {
                    return factory.apply(parent, callback, output, future);
                }

                @Override
                public LootContextParamSet getLootSet() {
                    return set;
                }
            };
            LOOT_TYPES.put(name, type);
            return type;
        }
    }

    private static final Map<String, LootType<?>> LOOT_TYPES = new HashMap<>();

    private final AbstractRegistrate<?> parent;

    private final Multimap<LootType<?>, Consumer<? super RegistrateLootTables>> specialLootActions = HashMultimap.create();
    private final Multimap<LootContextParamSet, Consumer<BiConsumer<ResourceKey<LootTable>, LootTable.Builder>>> lootActions = HashMultimap.create();
    private final Set<RegistrateLootTables> currentLootCreators = new HashSet<>();

    public RegistrateLootTableProvider(AbstractRegistrate<?> parent, FabricDataOutput output, CompletableFuture<HolderLookup.Provider> future) {
        super(output, Set.of(), List.of(), future);
        this.parent = parent;
        ((LootTableProviderAccessor) this).setSubProviders(getTables(output, future));
    }

    @Override
    public EnvType getSide() {
        return EnvType.SERVER;
    }

    @Override
    public void validate(MappedRegistry<LootTable> tables, ValidationContext context) {
        currentLootCreators.forEach(c -> c.validate(tables, context));
    }

    @SuppressWarnings("unchecked")
    public <T extends RegistrateLootTables> void addLootAction(LootType<T> type, NonNullConsumer<? extends RegistrateLootTables> action) {
        this.specialLootActions.put(type, (Consumer<? super RegistrateLootTables>) action);
    }

    public void addLootAction(LootContextParamSet set, Consumer<BiConsumer<ResourceKey<LootTable>, LootTable.Builder>> action) {
        this.lootActions.put(set, action);
    }

    private Function<HolderLookup.Provider, LootTableSubProvider> getLootCreator(AbstractRegistrate<?> parent, LootType<?> type, 
    		FabricDataOutput output, CompletableFuture<HolderLookup.Provider> future) {
        return (prov) -> {
            RegistrateLootTables creator = type.getLootCreator(parent, cons -> specialLootActions.get(type).forEach(c -> c.accept(cons)), output, future);
            currentLootCreators.add(creator);
            return creator;
        };
    }

    private static final BiMap<ResourceLocation, LootContextParamSet> SET_REGISTRY = LootContextParamSetsAccessor.getREGISTRY();

    public List<LootTableProvider.SubProviderEntry> getTables(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> future) {
        parent.genData(ProviderType.LOOT, this);
        currentLootCreators.clear();
        ImmutableList.Builder<LootTableProvider.SubProviderEntry> builder = ImmutableList.builder();
        for (LootType<?> type : LOOT_TYPES.values()) {
            builder.add(new SubProviderEntry(getLootCreator(parent, type, output, future), type.getLootSet()));
        }
        for (LootContextParamSet set : SET_REGISTRY.values()) {
            builder.add(new SubProviderEntry((prov) -> ((bicon) -> lootActions.get(set).forEach(a -> a.accept(bicon))), set));
        }
        return builder.build();
    }
}
