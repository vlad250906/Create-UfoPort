package io.github.fabricators_of_create.porting_lib.tool.mixin;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.google.common.collect.ImmutableList;

import java.util.List;

@Mixin(LootTable.Builder.class)
public interface BuilderAccessor {
	@Accessor
	ImmutableList.Builder<LootPool> getPools();
}
