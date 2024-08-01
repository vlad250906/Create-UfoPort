package com.simibubi.create.foundation.utility.fabric;

import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;

public interface AbstractMinecartExtensions {
	MinecartController create$getController();

	String CAP_KEY = "Controller";
}
