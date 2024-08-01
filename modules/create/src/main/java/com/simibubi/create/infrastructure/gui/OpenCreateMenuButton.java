package com.simibubi.create.infrastructure.gui;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

public class OpenCreateMenuButton extends Button {

	public static final ItemStack ICON = AllItems.GOGGLES.asStack();

	public OpenCreateMenuButton(int x, int y) {
		super(x, y, 20, 20, Components.immutableEmpty(), OpenCreateMenuButton::click, DEFAULT_NARRATION);
	}

	@Override
	public void renderString(GuiGraphics graphics, Font pFont, int pColor) {
		graphics.renderItem(ICON, getX() + 2, getY() + 2);
	}

	public static void click(Button b) {
		ScreenOpener.open(new CreateMainMenuScreen(Minecraft.getInstance().screen));
	}

	public static class SingleMenuRow {
		public final String left, right;

		public SingleMenuRow(String left, String right) {
			this.left = left;
			this.right = right;
		}

		public SingleMenuRow(String center) {
			this(center, center);
		}
	}

	public static class MenuRows {
		public static final MenuRows MAIN_MENU = new MenuRows(Arrays.asList(new SingleMenuRow("menu.singleplayer"),
				new SingleMenuRow("menu.multiplayer"), new SingleMenuRow("menu.online"),
				new SingleMenuRow("narrator.button.language", "narrator.button.accessibility")));

		public static final MenuRows INGAME_MENU = new MenuRows(Arrays.asList(new SingleMenuRow("menu.returnToGame"),
				new SingleMenuRow("gui.advancements", "gui.stats"),
				new SingleMenuRow("menu.sendFeedback", "menu.reportBugs"),
				new SingleMenuRow("menu.options", "menu.shareToLan"), new SingleMenuRow("menu.returnToMenu")));

		protected final List<String> leftButtons, rightButtons;

		public MenuRows(List<SingleMenuRow> variants) {
			leftButtons = variants.stream().map(r -> r.left).collect(Collectors.toList());
			rightButtons = variants.stream().map(r -> r.right).collect(Collectors.toList());
		}
	}

	public static class OpenConfigButtonHandler {

		public static void onGuiInit(Minecraft client, Screen gui, int scaledWidth, int scaledHeight) {
			MenuRows menu = null;
			int rowIdx = 0, offsetX = 0;
			if (gui instanceof TitleScreen) {
				menu = MenuRows.MAIN_MENU;
				rowIdx = AllConfigs.client().mainMenuConfigButtonRow.get();
				offsetX = AllConfigs.client().mainMenuConfigButtonOffsetX.get();
			} else if (gui instanceof PauseScreen) {
				menu = MenuRows.INGAME_MENU;
				rowIdx = AllConfigs.client().ingameMenuConfigButtonRow.get();
				offsetX = AllConfigs.client().ingameMenuConfigButtonOffsetX.get();
			}

			if (rowIdx != 0 && menu != null) {
				boolean onLeft = offsetX < 0;
				String target = (onLeft ? menu.leftButtons : menu.rightButtons).get(rowIdx - 1);

				int offsetX_ = offsetX;
				Screens.getButtons(gui).stream()
						.filter(w -> w.getMessage().getContents() instanceof TranslatableContents translatable
								&& translatable.getKey().equals(target))
						.findFirst().ifPresent(w -> {
							gui.addRenderableWidget(new OpenCreateMenuButton(
									w.getX() + offsetX_ + (onLeft ? -20 : w.getWidth()), w.getY()));
						});
			}
		}

	}

}
