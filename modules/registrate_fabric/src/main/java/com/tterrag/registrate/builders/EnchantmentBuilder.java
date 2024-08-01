package com.tterrag.registrate.builders;

import java.util.Arrays;
import java.util.EnumSet;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonnullType;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Cost;

/**
 * A builder for enchantments, allows for customization of the {@link Enchantment.Rarity enchantment rarity} and {@link EquipmentSlot equipment slots}, and configuration of data associated with
 * enchantments (lang).
 * 
 * @param <T>
 *            The type of enchantment being built
 * @param <P>
 *            Parent object type
 */
public class EnchantmentBuilder<T extends Enchantment, P> extends AbstractBuilder<Enchantment, T, P, EnchantmentBuilder<T, P>> {

    @FunctionalInterface
    public interface EnchantmentFactory<T extends Enchantment> {
        
        T create(Enchantment.EnchantmentDefinition def);
    }

    /**
     * Create a new {@link EnchantmentBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
     * <p>
     * The enchantment will be assigned the following data:
     * <ul>
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
     * @param type
     *            The {@link EnchantmentCategory type} of the enchantment
     * @param factory
     *            Factory to create the enchantment
     * @return A new {@link EnchantmentBuilder} with reasonable default data generators.
     */
    public static <T extends Enchantment, P> EnchantmentBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, HolderSet<Item> type, EnchantmentFactory<T> factory) {
        return new EnchantmentBuilder<>(owner, parent, name, callback, type, factory)
                .defaultLang();
    }

    private int weight = 10;
    private int maxLevel = 1;
    private int anvilCost = 1;
    private Cost minCost = new Cost(1, 10);
    private Cost maxCost = new Cost(6, 10);
    private final HolderSet<Item> type;
    
    @SuppressWarnings("null")
    private EnumSet<EquipmentSlotGroup> slots = EnumSet.noneOf(EquipmentSlotGroup.class);

    private final EnchantmentFactory<T> factory;

    protected EnchantmentBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, HolderSet<Item> type, EnchantmentFactory<T> factory) {
        super(owner, parent, name, callback, Registries.ENCHANTMENT);
        this.factory = factory;
        this.type = type;
    }

    /**
     * Set the rarity of this enchantment. Defaults to {@link Enchantment.Rarity#COMMON}.
     * 
     * @param rarity
     *            The rarity to assign
     * @return this {@link EnchantmentBuilder}
     */
    public EnchantmentBuilder<T, P> weight(int weight) {
        this.weight = weight;
        return this;
    }
    
    public EnchantmentBuilder<T, P> maxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
        return this;
    }
    
    public EnchantmentBuilder<T, P> anvilCost(int anvilCost) {
        this.anvilCost = anvilCost;
        return this;
    }
    
    public EnchantmentBuilder<T, P> minCost(int base, int perLevel) {
        this.minCost = new Cost(base, perLevel);
        return this;
    }
    
    public EnchantmentBuilder<T, P> maxCost(int base, int perLevel) {
        this.maxCost = new Cost(base, perLevel);
        return this;
    }
    
    public EnchantmentBuilder<T, P> weight(EnchantmentRarity weight) {
        this.weight = weight.getWeight();
        return this;
    }

    /**
     * Add the armor {@link EquipmentSlot slots} as valid slots for this enchantment, i.e. {@code HEAD}, {@code CHEST}, {@code LEGS}, and {@code FEET}.
     * 
     * @return this {@link EnchantmentBuilder}
     */
    public EnchantmentBuilder<T, P> addArmorSlots() {
        return addSlots(EquipmentSlotGroup.HEAD, EquipmentSlotGroup.CHEST, EquipmentSlotGroup.LEGS, EquipmentSlotGroup.FEET);
    }

    /**
     * Add valid slots for this enchantment. Defaults to none. Subsequent calls are additive.
     * 
     * @param slots
     *            The slots to add
     * @return this {@link EnchantmentBuilder}
     */
    public EnchantmentBuilder<T, P> addSlots(EquipmentSlotGroup... slots) {
        this.slots.addAll(Arrays.asList(slots));
        return this;
    }

    /**
     * Assign the default translation, as specified by {@link RegistrateLangProvider#getAutomaticName(NonNullSupplier, net.minecraft.resources.ResourceKey)}. This is the default, so it is generally not necessary to call, unless for
     * undoing previous changes.
     * 
     * @return this {@link EnchantmentBuilder}
     */
    public EnchantmentBuilder<T, P> defaultLang() {
        return lang(ench -> ench.description().getString());
    }

    /**
     * Set the translation for this enchantment.
     * 
     * @param name
     *            A localized English name
     * @return this {@link EnchantmentBuilder}
     */
    public EnchantmentBuilder<T, P> lang(String name) {
        return lang(ench -> ench.description().getString(), name);
    }

    @Override
    protected @NonnullType T createEntry() {
        return factory.create(Enchantment.definition(type, weight, maxLevel, minCost, maxCost, anvilCost, slots.toArray(new EquipmentSlotGroup[0])));
    }
    
    public static enum EnchantmentRarity {
        COMMON(10),
        UNCOMMON(5),
        RARE(2),
        VERY_RARE(1);

        private final int weight;

        private EnchantmentRarity(int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return this.weight;
        }
    }
}
