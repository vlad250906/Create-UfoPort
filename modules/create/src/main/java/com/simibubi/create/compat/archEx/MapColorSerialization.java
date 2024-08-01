package com.simibubi.create.compat.archEx;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.level.material.MapColor;

// https://github.com/DebuggyTeam/architecture-extensions/blob/1.20/src/main/java/io/github/debuggyteam/architecture_extensions/util/MapColors.java
public class MapColorSerialization {
	private static final Map<MapColor, String> names = new HashMap<>();

	static {
		// there's too many, so this is the bare minimum
		names.put(MapColor.COLOR_BLUE, "blue");
		names.put(MapColor.COLOR_RED, "red");
		names.put(MapColor.SAND, "sand");
		names.put(MapColor.TERRACOTTA_YELLOW, "yellow_terracotta");
		names.put(MapColor.COLOR_BROWN, "brown");
		names.put(MapColor.TERRACOTTA_GRAY, "gray_terracotta");
		names.put(MapColor.WARPED_NYLIUM, "warped_nylium");
		names.put(MapColor.DIRT, "dirt");
		names.put(MapColor.QUARTZ, "quartz");
		names.put(MapColor.STONE, "stone");
		names.put(MapColor.TERRACOTTA_WHITE, "white_terracotta");
		names.put(MapColor.TERRACOTTA_BROWN, "brown_terracotta");
		names.put(MapColor.DEEPSLATE, "deepslate");
	}

	public static String getArchExName(MapColor color) {
		String name = names.get(color);
		if (name == null)
			throw new IllegalArgumentException("Unsupported MapColor: " + color);
		return name;
	}
}
