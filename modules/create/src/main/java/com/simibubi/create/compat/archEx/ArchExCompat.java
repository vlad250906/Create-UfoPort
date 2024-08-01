package com.simibubi.create.compat.archEx;

import com.simibubi.create.Create;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator.Pack;

public class ArchExCompat {
	public static void init(Pack pack) {
		GroupProvider groupProvider = pack.addProvider(GroupProvider::new);
		new LangProvider(Create.ID, groupProvider.groups, Create.REGISTRATE::addRawLang).run();
	}
}
