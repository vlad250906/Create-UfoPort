package com.simibubi.create.content.redstone.thresholdSwitch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ThresholdSwitchScreen extends AbstractSimiScreen {

	private ScrollInput offBelow;
	private ScrollInput onAbove;
	private IconButton confirmButton;
	private IconButton flipSignals;

	private final Component invertSignal = Lang.translateDirect("gui.threshold_switch.invert_signal");
	private final ItemStack renderedItem = new ItemStack(AllBlocks.THRESHOLD_SWITCH.get());

	private AllGuiTextures background;
	private ThresholdSwitchBlockEntity blockEntity;
	private int lastModification;

	private LerpedFloat cursor;
	private LerpedFloat cursorLane;

	public ThresholdSwitchScreen(ThresholdSwitchBlockEntity be) {
		super(Lang.translateDirect("gui.threshold_switch.title"));
		background = AllGuiTextures.STOCKSWITCH;
		this.blockEntity = be;
		lastModification = -1;
	}

	@Override
	protected void init() {
		setWindowSize(background.width, background.height);
		setWindowOffset(-20, 0);
		super.init();

		int x = guiLeft;
		int y = guiTop;

		cursor = LerpedFloat.linear()
			.startWithValue(blockEntity.getLevelForDisplay());
		cursorLane = LerpedFloat.linear()
			.startWithValue(blockEntity.getState() ? 1 : 0);

		offBelow = new ScrollInput(x + 36, y + 42, 102, 18).withRange(0, 100)
			.titled(Components.empty())
			.calling(state -> {
				lastModification = 0;
				offBelow.titled(Lang.translateDirect("gui.threshold_switch.move_to_upper_at", state));
				if (onAbove.getState() <= state) {
					onAbove.setState(state + 1);
					onAbove.onChanged();
				}
			})
			.setState((int) (blockEntity.offWhenBelow * 100));

		onAbove = new ScrollInput(x + 36, y + 20, 102, 18).withRange(1, 101)
			.titled(Components.empty())
			.calling(state -> {
				lastModification = 0;
				onAbove.titled(Lang.translateDirect("gui.threshold_switch.move_to_lower_at", state));
				if (offBelow.getState() >= state) {
					offBelow.setState(state - 1);
					offBelow.onChanged();
				}
			})
			.setState((int) (blockEntity.onWhenAbove * 100));

		onAbove.onChanged();
		offBelow.onChanged();

		addRenderableWidget(onAbove);
		addRenderableWidget(offBelow);

		confirmButton = new IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			onClose();
		});
		addRenderableWidget(confirmButton);

		flipSignals = new IconButton(x + background.width - 62, y + background.height - 24, AllIcons.I_FLIP);
		flipSignals.withCallback(() -> {
			send(!blockEntity.isInverted());
		});
		flipSignals.setToolTip(invertSignal);
		addRenderableWidget(flipSignals);
	}

	@Override
	protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft;
		int y = guiTop;

		background.render(graphics, x, y);

		AllGuiTextures.STOCKSWITCH_POWERED_LANE.render(graphics, x + 37, y + (blockEntity.isInverted() ? 20 : 42));
		AllGuiTextures.STOCKSWITCH_UNPOWERED_LANE.render(graphics, x + 37, y + (blockEntity.isInverted() ? 42 : 20));
		graphics.drawString(font, title, x + (background.width - 8) / 2 - font.width(title) / 2, y + 4, 0x592424, false);

		AllGuiTextures sprite = AllGuiTextures.STOCKSWITCH_INTERVAL;
		float lowerBound = offBelow.getState();
		float upperBound = onAbove.getState();

		sprite.bind();
		graphics.blit(sprite.location, (int) (x + upperBound) + 37, y + 20, (int) (sprite.startX + upperBound), sprite.startY,
			(int) (sprite.width - upperBound), sprite.height);
		graphics.blit(sprite.location, x + 37, y + 42, sprite.startX, sprite.startY, (int) (lowerBound), sprite.height);

		AllGuiTextures.STOCKSWITCH_ARROW_UP.render(graphics, (int) (x + lowerBound + 36) - 2, y + 37);
		AllGuiTextures.STOCKSWITCH_ARROW_DOWN.render(graphics, (int) (x + upperBound + 36) - 3, y + 19);

		if (blockEntity.currentLevel != -1) {
			AllGuiTextures cursor = AllGuiTextures.STOCKSWITCH_CURSOR;
			PoseStack ms = graphics.pose();
			ms.pushPose();
			ms.translate(Math.min(99, this.cursor.getValue(partialTicks) * sprite.width),
				cursorLane.getValue(partialTicks) * 22, 0);
			cursor.render(graphics, x + 34, y + 21);
			ms.popPose();
		}

		GuiGameElement.of(renderedItem).<GuiGameElement
			.GuiRenderBuilder>at(x + background.width + 6, y + background.height - 56, -200)
			.scale(5)
			.render(graphics);
	}

	@Override
	public void tick() {
		super.tick();

		cursor.chase(blockEntity.getLevelForDisplay(), 1 / 4f, Chaser.EXP);
		cursor.tickChaser();
		cursorLane.chase(blockEntity.getState() ? 1 : 0, 1 / 4f, Chaser.EXP);
		cursorLane.tickChaser();

		if (lastModification >= 0)
			lastModification++;

		if (lastModification >= 20) {
			lastModification = -1;
			send(blockEntity.isInverted());
		}
	}

	@Override
	public void removed() {
		send(blockEntity.isInverted());
	}

	protected void send(boolean invert) {
		AllPackets.getChannel()
			.sendToServer(new ConfigureThresholdSwitchPacket(blockEntity.getBlockPos(), offBelow.getState() / 100f,
				onAbove.getState() / 100f, invert));
	}

}
