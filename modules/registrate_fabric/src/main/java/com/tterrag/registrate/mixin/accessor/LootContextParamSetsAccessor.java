package com.tterrag.registrate.mixin.accessor;

import com.google.common.collect.BiMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

@Mixin(LootContextParamSets.class)
public interface LootContextParamSetsAccessor {
    @Accessor
    static BiMap<ResourceLocation, LootContextParamSet> getREGISTRY() {
        throw new UnsupportedOperationException();
    }
}
