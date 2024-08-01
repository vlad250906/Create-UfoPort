package io.github.fabricators_of_create.porting_lib.data;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;

public abstract class PortingLibTagsProvider<T> extends FabricTagProvider<T> {
	
	private static final Map<ResourceKey<? extends Registry<?>>, String> CUSTOM_REGISTRY_DIRECTORIES = Map.of(Registries.BLOCK, "tags/block", Registries.ENTITY_TYPE, "tags/entity_type", Registries.FLUID, "tags/fluid", Registries.GAME_EVENT, "tags/game_event", Registries.ITEM, "tags/item");
	
	@Nullable
	protected final ExistingFileHelper existingFileHelper;
	private final ExistingFileHelper.IResourceType resourceType;
	private final ExistingFileHelper.IResourceType elementResourceType; // Resource type for validating required references to datapack registry elements.

	/**
	 * Constructs a new {@link FabricTagProvider} with the default computed path.
	 *
	 * <p>Common implementations of this class are provided.
	 *
	 * @param output           the {@link FabricDataOutput} instance
	 * @param registryKey
	 * @param registriesFuture the backing registry for the tag type
	 */
	public PortingLibTagsProvider(FabricDataOutput output, ResourceKey<? extends Registry<T>> registryKey, CompletableFuture<HolderLookup.Provider> registriesFuture, @Nullable ExistingFileHelper existingFileHelper) {
		super(output, registryKey, registriesFuture);
		this.existingFileHelper = existingFileHelper;
		this.resourceType = new ExistingFileHelper.ResourceType(PackType.SERVER_DATA, ".json", getTagDir(registryKey));
		this.elementResourceType = new ExistingFileHelper.ResourceType(PackType.SERVER_DATA, ".json", prefixNamespace(registryKey.location()));
	}
	
	public static String getTagDir(ResourceKey<? extends Registry<?>> registryKey) {
        String string = CUSTOM_REGISTRY_DIRECTORIES.get(registryKey);
        if (string != null) {
            return string;
        }
        return "tags/" + registryKey.location().getPath();
	}
	
	public static String prefixNamespace(ResourceLocation registryKey) {
		return registryKey.getNamespace().equals("minecraft") ? registryKey.getPath() : registryKey.getNamespace() +  "/"  + registryKey.getPath();
	}

	@Nullable
	protected Path getPath(ResourceLocation id) {
		return this.pathProvider.json(id);
	}

	public CompletableFuture<?> run(CachedOutput pOutput) {
		record CombinedData<T>(HolderLookup.Provider contents, TagsProvider.TagLookup<T> parent) {
		}
		return this.createContentsProvider().thenApply((p_275895_) -> {
			this.contentsDone.complete(null);
			return p_275895_;
		}).thenCombineAsync(this.parentProvider, CombinedData::new).thenCompose((combinedData) -> {
			HolderLookup.RegistryLookup<T> registrylookup = combinedData.contents.lookup(this.registryKey).orElseThrow(() -> {
				return new IllegalStateException("Registry " + this.registryKey.location() + " not found");
			});
			Predicate<ResourceLocation> predicate = (p_255496_) -> {
				return registrylookup.get(ResourceKey.create(this.registryKey, p_255496_)).isPresent();
			};
			Predicate<ResourceLocation> predicate1 = (p_274776_) -> {
				return this.builders.containsKey(p_274776_) || combinedData.parent.contains(TagKey.create(this.registryKey, p_274776_));
			};
			return CompletableFuture.allOf(this.builders.entrySet().stream().map((p_255499_) -> {
				ResourceLocation resourcelocation = p_255499_.getKey();
				TagBuilder tagbuilder = p_255499_.getValue();
				List<TagEntry> list = tagbuilder.build();
				List<TagEntry> list1 = list.stream().filter((tag) -> {
					return !tag.verifyIfPresent(predicate, predicate1);
				}).filter(this::missing).toList(); // Forge: Add validation via existing resources
				if (!list1.isEmpty()) {
					throw new IllegalArgumentException(String.format(Locale.ROOT, "Couldn't define tag %s as it is missing following references: %s", resourcelocation, list1.stream().map(Objects::toString).collect(Collectors.joining(","))));
				} else {
					JsonElement jsonelement = TagFile.CODEC.encodeStart(JsonOps.INSTANCE, new TagFile(list, false)).getOrThrow((e) -> {
						LOGGER.error(e);
						return null;
					});
					Path path = this.getPath(resourcelocation);
					if (path == null) return CompletableFuture.completedFuture(null); // Forge: Allow running this data provider without writing it. Recipe provider needs valid tags.
					return DataProvider.saveStable(pOutput, jsonelement, path);
				}
			}).toArray(CompletableFuture[]::new));
		});
	}

	private boolean missing(TagEntry reference) {
		// Optional tags should not be validated

		if (reference.required) {
			return existingFileHelper == null || !existingFileHelper.exists(reference.id, reference.tag ? resourceType : elementResourceType);
		}
		return false;
	}
}
