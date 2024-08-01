package com.tterrag.registrate.providers;

import java.util.Map;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;
import com.tterrag.registrate.util.nullness.FieldsAreNonnullByDefault;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a type of data that can be generated, and specifies a factory for
 * the provider.
 * <p>
 * Used as a key for data generator callbacks.
 * <p>
 * This file also defines the built-in provider types, but third-party types can
 * be created with {@link #register(String, ProviderType)}.
 *
 * @param <T> The type of the provider
 */
@FunctionalInterface
@SuppressWarnings("deprecation")
@FieldsAreNonnullByDefault
//@ParametersAreNonnullByDefault
public interface ProviderType<T extends RegistrateProvider> {

	// SERVER DATA
	public static final ProviderType<RegistrateRecipeProvider> RECIPE = register("recipe",
			(p, e) -> new RegistrateRecipeProvider(p, e.output(), e.registriesLookup()));
	public static final ProviderType<RegistrateAdvancementProvider> ADVANCEMENT = register("advancement",
			(p, e) -> new RegistrateAdvancementProvider(p, e.output(), e.registriesLookup()));
	public static final ProviderType<RegistrateLootTableProvider> LOOT = register("loot",
			(p, e) -> new RegistrateLootTableProvider(p, e.output(), e.registriesLookup()));
	public static final ProviderType<RegistrateTagsProvider.IntrinsicImpl<Block>> BLOCK_TAGS = register("tags/block",
			type -> (p, e) -> new RegistrateTagsProvider.IntrinsicImpl<Block>(p, type, "blocks", e.output(),
					Registries.BLOCK, e.registriesLookup(), block -> block.builtInRegistryHolder().key()));
	public static final ProviderType<RegistrateItemTagsProvider> ITEM_TAGS = registerDelegate("tags/item",
			type -> (p, e, existing) -> new RegistrateItemTagsProvider(p, type, "items", e.output(),
					e.registriesLookup(), ((TagsProvider<Block>) existing.get(BLOCK_TAGS)).contentsGetter(),
					e.helper()));
	public static final ProviderType<RegistrateTagsProvider.IntrinsicImpl<Fluid>> FLUID_TAGS = register("tags/fluid",
			type -> (p, e) -> new RegistrateTagsProvider.IntrinsicImpl<Fluid>(p, type, "fluids", e.output(),
					Registries.FLUID, e.registriesLookup(), fluid -> fluid.builtInRegistryHolder().key()));
	public static final ProviderType<RegistrateTagsProvider.IntrinsicImpl<EntityType<?>>> ENTITY_TAGS = register(
			"tags/entity",
			type -> (p, e) -> new RegistrateTagsProvider.IntrinsicImpl<EntityType<?>>(p, type, "entity_types",
					e.output(), Registries.ENTITY_TYPE, e.registriesLookup(),
					entityType -> entityType.builtInRegistryHolder().key()));

	// CLIENT DATA
	public static final ProviderType<RegistrateBlockstateProvider> BLOCKSTATE = register("blockstate",
			(p, e) -> new RegistrateBlockstateProvider(p, e.output(), e.helper()));
	public static final ProviderType<RegistrateItemModelProvider> ITEM_MODEL = register("item_model",
			(p, e, existing) -> new RegistrateItemModelProvider(p, e.output(),
					((RegistrateBlockstateProvider) existing.get(BLOCKSTATE)).getExistingFileHelper()));
	public static final ProviderType<RegistrateLangProvider> LANG = register("lang",
			(p, e) -> new RegistrateLangProvider(p, e.output(), e.registriesLookup()));

	T create(AbstractRegistrate<?> parent, RegistrateDataProvider.DataInfo info,
			Map<ProviderType<?>, RegistrateProvider> existing);

	// TODO this is clunky af
	@NotNull
	static <T extends RegistrateProvider> ProviderType<T> registerDelegate(String name,
			NonNullUnaryOperator<ProviderType<T>> type) {
		ProviderType<T> ret = new ProviderType<T>() {

			@Override
			public T create(@NotNull AbstractRegistrate<?> parent, RegistrateDataProvider.DataInfo info,
					Map<ProviderType<?>, RegistrateProvider> existing) {
				return type.apply(this).create(parent, info, existing);
			}
		};
		return register(name, ret);
	}

	@NotNull
	static <T extends RegistrateProvider> ProviderType<T> register(String name,
			NonNullFunction<ProviderType<T>, NonNullBiFunction<AbstractRegistrate<?>, RegistrateDataProvider.DataInfo, T>> type) {
		ProviderType<T> ret = new ProviderType<T>() {

			@Override
			public T create(@NotNull AbstractRegistrate<?> parent, RegistrateDataProvider.DataInfo info,
					Map<ProviderType<?>, RegistrateProvider> existing) {
				return type.apply(this).apply(parent, info);
			}
		};
		return register(name, ret);
	}

	@NotNull
	static <T extends RegistrateProvider> ProviderType<T> register(String name,
			NonNullBiFunction<AbstractRegistrate<?>, RegistrateDataProvider.DataInfo, T> type) {
		ProviderType<T> ret = new ProviderType<T>() {

			@Override
			public T create(AbstractRegistrate<?> parent, RegistrateDataProvider.DataInfo info,
					Map<ProviderType<?>, RegistrateProvider> existing) {
				return type.apply(parent, info);
			}
		};
		return register(name, ret);
	}

	@NotNull
	static <T extends RegistrateProvider> ProviderType<T> register(String name, ProviderType<T> type) {
		RegistrateDataProvider.TYPES.put(name, type);
		return type;
	}
}
