package com.tterrag.registrate.fabric;

import java.util.function.Consumer;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class RegistryUtil {
	public static void forAllRegistries(Consumer<Registry<?>> consumer) {
		// Fluid, Block, and Item need to run first
		consumer.accept(BuiltInRegistries.FLUID);
		consumer.accept(BuiltInRegistries.BLOCK);
		consumer.accept(BuiltInRegistries.ITEM);
		BuiltInRegistries.REGISTRY.forEach(registry -> {
			if (registry != BuiltInRegistries.FLUID && registry != BuiltInRegistries.BLOCK && registry != BuiltInRegistries.ITEM) {
				consumer.accept(registry);
			}
		});
	}
}
