package com.tterrag.registrate.fabric;

import java.util.Map;

import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public interface CustomValidationLootProvider {
    void validate(MappedRegistry<LootTable> tables, ValidationContext context);
}
