package com.tterrag.registrate.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

@Mixin(SpawnPlacements.class)
public interface SpawnPlacementsAccessor {
    @Invoker
    static <T extends Mob> void callRegister(EntityType<T> entityType, SpawnPlacementType type, Heightmap.Types types, SpawnPlacements.SpawnPredicate<T> spawnPredicate) {
        throw new UnsupportedOperationException();
    }
}
