package io.github.tropheusj.dripstone_fluid_lib;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

public class Shenanigans {
	private static final String intermediary = "net.minecraft.class_702$class_7759";
	private static final String mapped = FabricLoader.getInstance()
			.getMappingResolver()
			.mapClassName("intermediary", intermediary);
	private static final Class<?> clazz;
	private static final Constructor<?> constructor;

	static {
		try {
			clazz = Class.forName(mapped);
			constructor = clazz.getDeclaredConstructor(ResourceLocation.class, Optional.class);
			constructor.setAccessible(true);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			throw new RuntimeException("DripstoneFluidLib: Could not set up particle sprites", e);
		}
	}

	public static Object createReloadResult(ResourceLocation id, List<ResourceLocation> sprites) {
		Optional<List<ResourceLocation>> optional = Optional.of(sprites);
		try {
			return constructor.newInstance(id, optional);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("DripstoneFluidLib: failed to handle particle sprites", e);
		}
	}
}
