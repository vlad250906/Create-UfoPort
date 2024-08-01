package com.simibubi.create.foundation.utility;

import io.github.fabricators_of_create.porting_lib.util.FluidTextUtil;
import io.github.fabricators_of_create.porting_lib.util.FluidUnit;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.network.chat.MutableComponent;

public class FluidFormatter {

	public static String asString(long amount, boolean shorten, FluidUnit unit) {
		Couple<MutableComponent> couple = asComponents(amount, shorten, unit);
		return couple.getFirst().getString() + " " + couple.getSecond().getString();
	}

	public static Couple<MutableComponent> asComponents(long amount, boolean shorten, FluidUnit unit) {
		if (shorten && amount >= FluidConstants.BUCKET && unit == FluidUnit.MILLIBUCKETS) {
			return Couple.create(Components.literal(String.format("%.1f", amount / (double) FluidConstants.BUCKET)),
					Lang.translateDirect("generic.unit.buckets"));
		}

		String amountToDisplay = FluidTextUtil.getUnicodeMillibuckets(amount, unit, true);
		return Couple.create(Components.literal(amountToDisplay), Lang.translateDirect(unit.getTranslationKey()));
	}

}
