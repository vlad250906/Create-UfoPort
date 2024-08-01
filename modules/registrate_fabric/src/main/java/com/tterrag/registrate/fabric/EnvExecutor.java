package com.tterrag.registrate.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import java.util.function.Supplier;

public class EnvExecutor {
	public static void runWhenOn(EnvType env, Supplier<Runnable> toRun) {
		if (FabricLoader.getInstance().getEnvironmentType() == env) {
			toRun.get().run();
		}
	}
}
