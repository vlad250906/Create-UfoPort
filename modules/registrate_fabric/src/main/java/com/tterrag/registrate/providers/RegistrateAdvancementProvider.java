package com.tterrag.registrate.providers;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.JsonOps;
import com.tterrag.registrate.AbstractRegistrate;
import net.fabricmc.api.EnvType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class RegistrateAdvancementProvider implements RegistrateProvider, Consumer<AdvancementHolder> {
	
	private static final Log log = 
		    LogFactory.getLog(RegistrateAdvancementProvider.class);

    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

    private final AbstractRegistrate<?> owner;
    private final PackOutput packOutput;
    private final CompletableFuture<Provider> registriesLookup;
    private final List<CompletableFuture<?>> advancementsToSave = Lists.newArrayList();

    public RegistrateAdvancementProvider(AbstractRegistrate<?> owner, PackOutput packOutputIn, CompletableFuture<HolderLookup.Provider> registriesLookupIn) {
        this.owner = owner;
        this.packOutput = packOutputIn;
        this.registriesLookup = registriesLookupIn;
    }

    @Override
    public EnvType getSide() {
        return EnvType.SERVER;
    }

    public MutableComponent title(String category, String name, String title) {
        return owner.addLang("advancements", ResourceLocation.fromNamespaceAndPath(category, name), "title", title);
    }

    public MutableComponent desc(String category, String name, String desc) {
        return owner.addLang("advancements", ResourceLocation.fromNamespaceAndPath(category, name), "description", desc);
    }

    private @Nullable CachedOutput cache;
    private Set<ResourceLocation> seenAdvancements = new HashSet<>();

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registriesLookup.thenCompose(lookup -> {
            advancementsToSave.clear();

            try {
                this.cache = cache;
                this.seenAdvancements.clear();
                owner.genData(ProviderType.ADVANCEMENT, this);
            } finally {
                this.cache = null;
            }

            return CompletableFuture.allOf(advancementsToSave.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public void accept(@Nullable AdvancementHolder t) {
        CachedOutput cache = this.cache;
        if (cache == null) {
            throw new IllegalStateException("Cannot accept advancements outside of act");
        }
        Objects.requireNonNull(t, "Cannot accept a null advancement");
        Path path = this.packOutput.getOutputFolder();
        if (!seenAdvancements.add(t.id())) {
            throw new IllegalStateException("Duplicate advancement " + t.id());
        } else {
            Path path1 = getPath(path, t);
            advancementsToSave.add(DataProvider.saveStable(cache, Advancement.CODEC.encodeStart(JsonOps.INSTANCE, t.value()).result().get(), path1));
        }
    }

    private static Path getPath(Path pathIn, AdvancementHolder advancementIn) {
        return pathIn.resolve("data/" + advancementIn.id().getNamespace() + "/advancements/" + advancementIn.id().getPath() + ".json");
    }

    public String getName() {
        return "Advancements";
    }
}
