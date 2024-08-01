package com.tterrag.registrate.builders;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.collect.Maps;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.fabric.EnvExecutor;
import com.tterrag.registrate.fabric.RegistryObject;
import com.tterrag.registrate.providers.*;
import com.tterrag.registrate.util.CreativeModeTabModifier;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/**
 * A builder for items, allows for customization of the {@link Item.Properties} and configuration of data associated with items (models, recipes, etc.).
 *
 * @param <T>
 *            The type of item being built
 * @param <P>
 *            Parent object type
 */
public class ItemBuilder<T extends Item, P> extends AbstractBuilder<Item, T, P, ItemBuilder<T, P>> {

    /**
     * Create a new {@link ItemBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
     * <p>
     * The item will be assigned the following data:
     * <ul>
     * <li>A simple generated model with one texture (via {@link #defaultModel()})</li>
     * <li>The default translation (via {@link #defaultLang()})</li>
     * </ul>
     *
     * @param <T>
     *            The type of the builder
     * @param <P>
     *            Parent object type
     * @param owner
     *            The owning {@link AbstractRegistrate} object
     * @param parent
     *            The parent object
     * @param name
     *            Name of the entry being built
     * @param callback
     *            A callback used to actually register the built entry
     * @param factory
     *            Factory to create the item
     * @return A new {@link ItemBuilder} with reasonable default data generators.
     */
    public static <T extends Item, P> ItemBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<Item.Properties, T> factory) {
        return new ItemBuilder<>(owner, parent, name, callback, factory)
                .defaultModel().defaultLang();
    }

    private final NonNullFunction<Item.Properties, T> factory;

    private NonNullSupplier<Item.Properties> initialProperties = Item.Properties::new;
    private NonNullFunction<Item.Properties, Item.Properties> propertiesCallback = NonNullUnaryOperator.identity();

    @Nullable
    private NonNullSupplier<Supplier<ItemColor>> colorHandler;
    private Map<ResourceKey<CreativeModeTab>, Consumer<CreativeModeTabModifier>> creativeModeTabs = Maps.newLinkedHashMap();

    protected ItemBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<Item.Properties, T> factory) {
        super(owner, parent, name, callback, Registries.ITEM);
        this.factory = factory;

        onRegister(item -> {
            creativeModeTabs.forEach(owner::modifyCreativeModeTab);
            creativeModeTabs.clear(); // this registration should only fire once, to doubly ensure this, clear the map
        });
    }

    /**
     * Modify the properties of the item. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
     * different operations.
     * <p>
     * If a different properties instance is returned, it will replace the existing one entirely.
     *
     * @param func
     *            The action to perform on the properties
     * @return this {@link ItemBuilder}
     */
    public ItemBuilder<T, P> properties(NonNullUnaryOperator<Item.Properties> func) {
        propertiesCallback = propertiesCallback.andThen(func);
        return this;
    }

    /**
     * Replace the initial state of the item properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
     *
     * @param properties
     *            A supplier to to create the initial properties
     * @return this {@link ItemBuilder}
     */
    public ItemBuilder<T, P> initialProperties(NonNullSupplier<Item.Properties> properties) {
        initialProperties = properties;
        return this;
    }

    /**
     * Adds the item built from this builder into the given CreativeModeTab using the specified modifier
     *
     * <p>
     * CreativeModeTab registration is delegated off until the item has been finalized and registered to the {@link net.minecraft.core.registries.BuiltInRegistries#ITEM} registry.<br>
     * This means you can call this method as many times as you like during the build process with no added side effects.
     * <p>
     * Calling this method with different {@link CreativeModeTab tabs} will add your item to all the specified tabs,
     * unlike the old implementation which only allowed you to specify a single tab to display your times on.
     * <p>
     * Calling this method multiple times with the same {@link NonNullSupplier tab supplier} will replace any previous calls.
     *
     * @param tab The {@link CreativeModeTab} to add the item into
     * @param modifier The {@link CreativeModeTabModifier} used to build the ItemStack
     * @return This builder
     */
    public ItemBuilder<T, P> tab(ResourceKey<CreativeModeTab> tab, Consumer<CreativeModeTabModifier> modifier) {
        creativeModeTabs.put(tab, modifier); // Should we get the current value in the map [if one exists] and .andThen() the 2 together? right now we replace any consumer that currently exists
        return this;
    }

    /**
     * Adds the item built from this builder into the given CreativeModeTab using the default ItemStack instance
     *
     * <p>
     * CreativeModeTab registration is delegated off until the item has been finalized and registered to the {@link net.minecraft.core.registries.BuiltInRegistries#ITEM} registry.<br>
     * This means you can call this method as many times as you like during the build process with no added side effects.
     * <p>
     * Calling this method with different {@link CreativeModeTab tabs} will add your item to all the specified tabs,
     * unlike the old implementation which only allowed you to specify a single tab to display your times on.
     * <p>
     * Calling this method multiple times with the same {@link NonNullSupplier tab supplier} will replace any previous calls.
     *
     * @param tab The {@link CreativeModeTab} to add the item into
     * @return This builder
     * @see #tab(ResourceKey, Consumer)
     */
    public ItemBuilder<T, P> tab(ResourceKey<CreativeModeTab> tab) {
        return tab(tab, modifier -> modifier.accept(get()));
    }

    /**
     * Removes the item built from this builder from the given CreativeModeTab
     *
     * @param tab The {@link CreativeModeTab} to remove the item from
     * @return This builder
     */
    public ItemBuilder<T, P> removeTab(ResourceKey<CreativeModeTab> tab) {
        creativeModeTabs.remove(tab);
        return this;
    }

    /**
     * Register a block color handler for this item. The {@link ItemColor} instance can be shared across many items.
     *
     * @param colorHandler
     *            The color handler to register for this item
     * @return this {@link ItemBuilder}
     */
    public ItemBuilder<T, P> color(NonNullSupplier<Supplier<ItemColor>> colorHandler) {
        if (this.colorHandler == null) {
            EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::registerItemColor);
        }
        this.colorHandler = colorHandler;
        return this;
    }

    protected void registerItemColor() {
        onRegister(entry -> {
            ColorProviderRegistry.ITEM.register(colorHandler.get().get(), entry);
        });
    }

    /**
     * Assign the default model to this item, which is simply a generated model with a single texture of the same name.
     *
     * @return this {@link ItemBuilder}
     */
    public ItemBuilder<T, P> defaultModel() {
        return model((ctx, prov) -> prov.generated(ctx::getEntry));
    }

    /**
     * Configure the model for this item.
     *
     * @param cons
     *            The callback which will be invoked during data creation
     * @return this {@link ItemBuilder}
     * @see #setData(ProviderType, NonNullBiConsumer)
     */
    public ItemBuilder<T, P> model(NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> cons) {
        return setData(ProviderType.ITEM_MODEL, cons);
    }

    /**
     * Assign the default translation, as specified by {@link RegistrateLangProvider#getAutomaticName(NonNullSupplier, net.minecraft.resources.ResourceKey)}. This is the default, so it is generally
     * not necessary to call, unless for undoing previous changes.
     *
     * @return this {@link ItemBuilder}
     */
    public ItemBuilder<T, P> defaultLang() {
        return lang(Item::getDescriptionId);
    }

    /**
     * Set the translation for this item.
     *
     * @param name
     *            A localized English name
     * @return this {@link ItemBuilder}
     */
    public ItemBuilder<T, P> lang(String name) {
        return lang(Item::getDescriptionId, name);
    }

    /**
     * Configure the recipe(s) for this item.
     *
     * @param cons
     *            The callback which will be invoked during data generation.
     * @return this {@link ItemBuilder}
     * @see #setData(ProviderType, NonNullBiConsumer)
     */
    public ItemBuilder<T, P> recipe(NonNullBiConsumer<DataGenContext<Item, T>, RegistrateRecipeProvider> cons) {
        return setData(ProviderType.RECIPE, cons);
    }

    /**
     * Assign {@link TagKey}{@code s} to this item. Multiple calls will add additional tags.
     *
     * @param tags
     *            The tag to assign
     * @return this {@link ItemBuilder}
     */
    @SafeVarargs
    public final ItemBuilder<T, P> tag(TagKey<Item>... tags) {
        return tag(ProviderType.ITEM_TAGS, tags);
    }

    @Override
    protected T createEntry() {
        Item.Properties properties = this.initialProperties.get();
        properties = propertiesCallback.apply(properties);
        return factory.apply(properties);
    }

    @Override
    protected RegistryEntry<T> createEntryWrapper(RegistryObject<T> delegate) {
        return new ItemEntry<>(getOwner(), delegate);
    }

    @Override
    public ItemEntry<T> register() {
        return (ItemEntry<T>) super.register();
    }
}