package com.simibubi.create.foundation.ponder;

import java.util.List;

import com.google.common.base.Strings;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.ponder.ui.NavigatableSimiScreen;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

import io.github.fabricators_of_create.porting_lib.event.client.RenderTooltipBorderColorCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class PonderTooltipHandler {

	public static boolean enable = true;

	static LerpedFloat holdWProgress = LerpedFloat.linear()
		.startWithValue(0);
	static ItemStack hoveredStack = ItemStack.EMPTY;
	static ItemStack trackingStack = ItemStack.EMPTY;
	static boolean subject = false;
	static boolean deferTick = false;

	public static final String HOLD_TO_PONDER = PonderLocalization.LANG_PREFIX + "hold_to_ponder";
	public static final String SUBJECT = PonderLocalization.LANG_PREFIX + "subject";

	public static void tick() {
		deferTick = true;
	}

	public static void deferredTick() {
		deferTick = false;
		Minecraft instance = Minecraft.getInstance();
		Screen currentScreen = instance.screen;

		if (hoveredStack.isEmpty() || trackingStack.isEmpty()) {
			trackingStack = ItemStack.EMPTY;
			holdWProgress.startWithValue(0);
			return;
		}

		float value = holdWProgress.getValue();
		int keyCode = KeyBindingHelper.getBoundKeyOf(ponderKeybind()).getValue();
		long window = instance.getWindow()
			.getWindow();

		if (!subject && InputConstants.isKeyDown(window, keyCode)) {
			if (value >= 1) {
				if (currentScreen instanceof NavigatableSimiScreen)
					((NavigatableSimiScreen) currentScreen).centerScalingOnMouse();
				ScreenOpener.transitionTo(PonderUI.of(trackingStack));
				holdWProgress.startWithValue(0);
				return;
			}
			holdWProgress.setValue(Math.min(1, value + Math.max(.25f, value) * .25f));
		} else
			holdWProgress.setValue(Math.max(0, value - .05f));

		hoveredStack = ItemStack.EMPTY;
	}

	public static void addToTooltip(ItemStack stack, List<Component> tooltip) {
		if (!enable)
			return;

		updateHovered(stack);

		if (deferTick)
			deferredTick();

		if (trackingStack != stack)
			return;

		float renderPartialTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
		Component component = subject ? Lang.translateDirect(SUBJECT)
			.withStyle(ChatFormatting.GREEN)
			: makeProgressBar(Math.min(1, holdWProgress.getValue(renderPartialTicks) * 8 / 7f));
		if (tooltip.size() < 2)
			tooltip.add(component);
		else
			tooltip.add(1, component);
	}

	protected static void updateHovered(ItemStack stack) {
		Minecraft instance = Minecraft.getInstance();
		Screen currentScreen = instance.screen;
		boolean inPonderUI = currentScreen instanceof PonderUI;

		ItemStack prevStack = trackingStack;
		hoveredStack = ItemStack.EMPTY;
		subject = false;

		if (inPonderUI) {
			PonderUI ponderUI = (PonderUI) currentScreen;
			if (ItemHelper.sameItem(stack, ponderUI.getSubject()))
				subject = true;
		}

		if (stack.isEmpty())
			return;
		if (!PonderRegistry.ALL.containsKey(RegisteredObjects.getKeyOrThrow(stack.getItem())))
			return;

		if (prevStack.isEmpty() || !ItemHelper.sameItem(prevStack, stack))
			holdWProgress.startWithValue(0);

		hoveredStack = stack;
		trackingStack = stack;
	}

	public static RenderTooltipBorderColorCallback.BorderColorEntry handleTooltipColor(ItemStack stack, int originalBorderColorStart, int originalBorderColorEn) {
		if (trackingStack != stack)
			return null;
		if (holdWProgress.getValue() == 0)
			return null;
		float renderPartialTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
		int start = originalBorderColorStart;
		int end = originalBorderColorEn;
		float progress = Math.min(1, holdWProgress.getValue(renderPartialTicks) * 8 / 7f);

		start = getSmoothColorForProgress(progress);
		end = getSmoothColorForProgress((progress));

		return new RenderTooltipBorderColorCallback.BorderColorEntry(start | 0xa0000000, end | 0xa0000000);
	}

	private static int getSmoothColorForProgress(float progress) {
		if (progress < .5f)
			return Color.mixColors(0x5000FF, 5592575, progress * 2);
		return Color.mixColors(5592575, 0xffffff, (progress - .5f) * 2);
	}

	private static Component makeProgressBar(float progress) {
		MutableComponent holdW = Lang
			.translateDirect(HOLD_TO_PONDER,
				((MutableComponent) ponderKeybind().getTranslatedKeyMessage()).withStyle(ChatFormatting.GRAY))
			.withStyle(ChatFormatting.DARK_GRAY);

		Font fontRenderer = Minecraft.getInstance().font;
		float charWidth = fontRenderer.width("|");
		float tipWidth = fontRenderer.width(holdW);

		int total = (int) (tipWidth / charWidth);
		int current = (int) (progress * total);

		if (progress > 0) {
			String bars = "";
			bars += ChatFormatting.GRAY + Strings.repeat("|", current);
			if (progress < 1)
				bars += ChatFormatting.DARK_GRAY + Strings.repeat("|", total - current);
			return Components.literal(bars);
		}

		return holdW;
	}

	protected static KeyMapping ponderKeybind() {
		return Minecraft.getInstance().options.keyUp;
	}

}
