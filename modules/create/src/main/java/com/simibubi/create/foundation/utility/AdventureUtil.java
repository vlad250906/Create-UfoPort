package com.simibubi.create.foundation.utility;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;

public class AdventureUtil {
	public static boolean isAdventure(@Nullable Player player) {
		return player != null && !player.mayBuild() && !player.isSpectator();
	}
}
