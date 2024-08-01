package com.simibubi.create.compat.archEx;

import java.util.Locale;

public enum BlockType {
	// stone
	ARCH,
	OCTAGONAL_COLUMN,
	ROUND_ARCH,
	ROOF,
	WALL_COLUMN,
	WALL_POST,
	// wood
	BEAM,
	CROWN_MOLDING,
	FENCE_POST,
	JOIST,
	LATTICE,
	ROUND_FENCE_POST,
	TRANSOM,
	POST_CAP,
	POST_LANTERN,
	// metal
	H_BEAM,
	I_BEAM,
	TUBE_METAL,
	// all
	FACADE
	;

	public static final BlockType[] FOR_STONES = { ARCH, OCTAGONAL_COLUMN, ROUND_ARCH, ROOF, WALL_COLUMN, WALL_POST, FACADE };

	public final String name;

	BlockType() {
		this.name = name().toLowerCase(Locale.ROOT);
	}

	BlockType(String name) {
		this.name = name;
	}
}
