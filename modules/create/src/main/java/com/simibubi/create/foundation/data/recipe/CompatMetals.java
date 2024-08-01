package com.simibubi.create.foundation.data.recipe;

import static com.simibubi.create.foundation.data.recipe.Mods.*;

import com.simibubi.create.foundation.utility.Lang;

public enum CompatMetals {
	ALUMINUM(IE),
	LEAD(MEK, TH, IE, TR, MI),
	NICKEL(TH, IE, MI, ALG),
	OSMIUM(MEK, MTM),
	PLATINUM(MTM, MI),
	QUICKSILVER,
	SILVER(TH, IE, TR, MI, MTM),
	TIN(TH, MEK, TR, MI, ALG, MTM),
	URANIUM(MEK, IE, MI);

	private final Mods[] mods;
	private final String name;

	CompatMetals(Mods... mods) {
		this.name = Lang.asId(name());
		this.mods = mods;
	}

	public String getName() {
		return name;
	}

	/**
	 * These mods must provide an ingot and nugget variant of the corresponding metal.
	 */
	public Mods[] getMods() {
		return mods;
	}
}
