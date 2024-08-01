package com.simibubi.create.foundation.mixin.fabric;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.Gui;

@Mixin(Gui.class)
public interface GuiAccessor {
	@Accessor
	int getToolHighlightTimer();
}
