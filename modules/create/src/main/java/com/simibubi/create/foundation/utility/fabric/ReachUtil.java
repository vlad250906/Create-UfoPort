package com.simibubi.create.foundation.utility.fabric;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class ReachUtil {
	public static double reach(Player p) {
		return p.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
		//return ReachEntityAttributes.getReachDistance(p, p.isCreative() ? 5 : 4.5);
	}
}
