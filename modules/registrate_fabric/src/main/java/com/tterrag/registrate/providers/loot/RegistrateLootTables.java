package com.tterrag.registrate.providers.loot;

import net.minecraft.core.MappedRegistry;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public interface RegistrateLootTables extends LootTableSubProvider
{

    default void validate(MappedRegistry<LootTable> map, ValidationContext validationresults) {}

}
