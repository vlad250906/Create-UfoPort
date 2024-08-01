package com.simibubi.create.foundation.gui;

import java.util.Collection;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.utility.Components;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.ScreenAccessor;
import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public abstract class AbstractSimiScreen extends Screen {

	protected int windowWidth, windowHeight;
	protected int windowXOffset, windowYOffset;
	protected int guiLeft, guiTop;

	protected AbstractSimiScreen(Component title) {
		super(title);
	}

	protected AbstractSimiScreen() {
		this(Components.immutableEmpty());
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowSize(int width, int height) {
		windowWidth = width;
		windowHeight = height;
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowOffset(int xOffset, int yOffset) {
		windowXOffset = xOffset;
		windowYOffset = yOffset;
	}

	@Override
	protected void init() {
		guiLeft = (width - windowWidth) / 2;
		guiTop = (height - windowHeight) / 2;
		guiLeft += windowXOffset;
		guiTop += windowYOffset;
	}

	@Override
	public void tick() {
		for (GuiEventListener listener : children()) {
			if (listener instanceof TickableGuiEventListener tickable) {
				tickable.tick();
			}
		}
	}

	@Override
	public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		if (getFocused() != null && !getFocused().isMouseOver(pMouseX, pMouseY))
			setFocused(null);
		return super.mouseClicked(pMouseX, pMouseY, pButton);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@SuppressWarnings("unchecked")
	protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(W... widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(Collection<W> widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected void removeWidgets(GuiEventListener... widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	protected void removeWidgets(Collection<? extends GuiEventListener> widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
		PoseStack ms = graphics.pose();

		ms.pushPose();

		prepareFrame();

		renderWindowBackground(graphics, mouseX, mouseY, partialTicks);
		renderWindow(graphics, mouseX, mouseY, partialTicks);
		super.render(graphics, mouseX, mouseY, partialTicks);
		renderWindowForeground(graphics, mouseX, mouseY, partialTicks);

		endFrame();

		ms.popPose();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		boolean keyPressed = super.keyPressed(keyCode, scanCode, modifiers);
		if (keyPressed || getFocused() instanceof EditBox)
			return keyPressed;

		InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
		if (KeyBindingHelper.isActiveAndMatches(this.minecraft.options.keyInventory, mouseKey)) {
			this.onClose();
			return true;
		}

		return false;
	}

	protected void prepareFrame() {
	}

	protected void renderWindowBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		renderBackground(graphics, mouseX, mouseY, partialTicks);
	}

	protected abstract void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);

	protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		for (Renderable widget : ((ScreenAccessor) this).port_lib$getRenderables()) {
			if (widget instanceof AbstractSimiWidget simiWidget && simiWidget.isMouseOver(mouseX, mouseY)
					&& simiWidget.visible) {
				List<Component> tooltip = simiWidget.getToolTip();
				if (tooltip.isEmpty())
					continue;
				int ttx = simiWidget.lockedTooltipX == -1 ? mouseX : simiWidget.lockedTooltipX + simiWidget.getX();
				int tty = simiWidget.lockedTooltipY == -1 ? mouseY : simiWidget.lockedTooltipY + simiWidget.getY();
				graphics.renderComponentTooltip(font, tooltip, ttx, tty);
			}
		}
	}

	protected void endFrame() {
	}

	@Deprecated
	protected void debugWindowArea(GuiGraphics graphics) {
		graphics.fill(guiLeft + windowWidth, guiTop + windowHeight, guiLeft, guiTop, 0xD3D3D3D3);
	}

	@Override
	public GuiEventListener getFocused() {
		GuiEventListener focused = super.getFocused();
		if (focused instanceof AbstractWidget && !((AbstractWidget) focused).isFocused())
			focused = null;
		setFocused(focused);
		return focused;
	}

}
