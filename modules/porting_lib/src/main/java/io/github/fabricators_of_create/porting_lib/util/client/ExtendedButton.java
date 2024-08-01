package io.github.fabricators_of_create.porting_lib.util.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * This class provides a button that fixes several bugs present in the vanilla GuiButton drawing code.
 * The gist of it is that it allows buttons of any size without gaps in the graphics and with the
 * borders drawn properly. It also prevents button text from extending out of the sides of the button by
 * trimming the end of the string and adding an ellipsis.<br/><br/>
 *
 * The code that handles drawing the button is in GuiUtils.
 *
 * @author bspkrs
 */
public class ExtendedButton extends Button {
	public ExtendedButton(int xPos, int yPos, int width, int height, Component displayString, OnPress handler) {
		this(xPos, yPos, width, height, displayString, handler, DEFAULT_NARRATION);
	}

	public ExtendedButton(int xPos, int yPos, int width, int height, Component displayString, OnPress handler, CreateNarration createNarration) {
		super(xPos, yPos, width, height, displayString, handler, createNarration);
	}

	/**
	 * Draws this button to the screen.
	 */
	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		int k = this.getY();
		ScreenUtils.drawContinuousTexturedBox(guiGraphics, SPRITES.get(true, true), this.getX(), this.getY(), 0, 46 + k * 20, this.width, this.height, 200, 20, 2, 3, 2, 2, 0);

		Component buttonText = this.getMessage();
		int strWidth = mc.font.width(buttonText);
		int ellipsisWidth = mc.font.width("...");

		if (strWidth > width - 6 && strWidth > ellipsisWidth)
			//TODO, srg names make it hard to figure out how to append to an ITextProperties from this trim operation, wraping this in StringTextComponent is kinda dirty.
			buttonText = Component.literal(mc.font.substrByWidth(buttonText, width - 6 - ellipsisWidth).getString() + "...");

		guiGraphics.drawCenteredString(mc.font, buttonText, this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, this.active ? 16777215 : 10526880);
	}
}
