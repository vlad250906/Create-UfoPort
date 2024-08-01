package com.simibubi.create.compat.archEx;

import net.minecraft.resources.ResourceLocation;

public record Textures(String typeOrId) {
	public static final Textures LOG = new Textures("wood_with_log");
	public static final Textures STEM = new Textures("wood_with_stem");
	public static final Textures TOP_SIDE_BOTTOM = new Textures("top_bottom");
	public static final Textures TOP_SIDE = new Textures("top");
	public static final Textures SINGLE = new Textures("all");

	public static Textures ofTexture(String texture) {
		return new Textures("create:" + texture);
	}

	public static Textures ofTexture(ResourceLocation id) {
		return new Textures(id.toString());
	}
}
