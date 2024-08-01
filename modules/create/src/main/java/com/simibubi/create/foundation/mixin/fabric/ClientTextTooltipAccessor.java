package com.simibubi.create.foundation.mixin.fabric;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;

@Mixin(ClientTextTooltip.class)
public interface ClientTextTooltipAccessor {
	@Accessor("text")
	FormattedCharSequence create$text();
}
