package com.simibubi.create.foundation.config.ui.compat.flywheel;

import com.jozufozu.flywheel.config.Option;
import com.mojang.blaze3d.vertex.PoseStack;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.Theme;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.element.RenderElement;
import com.simibubi.create.foundation.gui.widget.BoxWidget;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.AbstractWidgetAccessor;
import net.minecraft.client.gui.GuiGraphics;

public class FlwBooleanEntry extends FlwValueEntry<Boolean> {
	RenderElement enabled;
	RenderElement disabled;
	BoxWidget button;

	public FlwBooleanEntry(String label, Option<Boolean> option) {
		super(label, option);

		enabled = AllIcons.I_CONFIRM.asStencil().withElementRenderer((ms, width, height, alpha) -> UIRenderHelper
				.angledGradient(ms, 0, 0, height / 2, height, width, Theme.p(Theme.Key.BUTTON_SUCCESS))).at(10, 0);

		disabled = AllIcons.I_DISABLE.asStencil().withElementRenderer((ms, width, height, alpha) -> UIRenderHelper
				.angledGradient(ms, 0, 0, height / 2, height, width, Theme.p(Theme.Key.BUTTON_FAIL))).at(10, 0);

		button = new BoxWidget().showingElement(enabled).withCallback(() -> setValue(!getValue()));

		listeners.add(button);
		onReset();
	}

	@Override
	public void setEditable(boolean b) {
		super.setEditable(b);
		button.active = b;
	}

	@Override
	public void tick() {
		super.tick();
		button.tick();
	}

	@Override
	public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY,
			boolean p_230432_9_, float partialTicks) {
		super.render(graphics, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);

		button.setX(x + width - 80 - resetWidth);
		button.setY(y + 10);
		button.setWidth(35);
		((AbstractWidgetAccessor) button).port_lib$setHeight(height - 20);
		button.render(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public void onValueChange(Boolean newValue) {
		super.onValueChange(newValue);
		button.showingElement(newValue ? enabled : disabled);
		bumpCog(newValue ? 15f : -16f);
	}

}
