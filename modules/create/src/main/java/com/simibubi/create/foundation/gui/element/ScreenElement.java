package com.simibubi.create.foundation.gui.element;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;

public interface ScreenElement {

	@Environment(EnvType.CLIENT)
	void render(GuiGraphics graphics, int x, int y);

}
