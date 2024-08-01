package com.simibubi.create.content.equipment.zapper;

import java.util.Vector;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ZapperScreen extends AbstractSimiScreen {

	protected final Component patternSection = Lang.translateDirect("gui.terrainzapper.patternSection");

	protected AllGuiTextures background;
	protected ItemStack zapper;
	protected InteractionHand hand;

	protected float animationProgress;

	protected Component title;
	protected Vector<IconButton> patternButtons = new Vector<>(6);
	private IconButton confirmButton;
	protected int brightColor;
	protected int fontColor;

	protected PlacementPatterns currentPattern;

	public ZapperScreen(AllGuiTextures background, ItemStack zapper, InteractionHand hand) {
		this.background = background;
		this.zapper = zapper;
		this.hand = hand;
		title = Components.immutableEmpty();
		brightColor = 0xFEFEFE;
		fontColor = AllGuiTextures.FONT_COLOR;

		CompoundTag nbt = ItemHelper.getOrCreateComponent(zapper, AllDataComponents.ZAPPER, new CompoundTag());
		currentPattern = NBTHelper.readEnum(nbt, "Pattern", PlacementPatterns.class);
	}

	@Override
	protected void init() {
		setWindowSize(background.width, background.height);
		setWindowOffset(-10, 0);
		super.init();

		animationProgress = 0;

		int x = guiLeft;
		int y = guiTop;

		confirmButton =
			new IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			onClose();
		});
		addRenderableWidget(confirmButton);

		patternButtons.clear();
		for (int row = 0; row <= 1; row++) {
			for (int col = 0; col <= 2; col++) {
				int id = patternButtons.size();
				PlacementPatterns pattern = PlacementPatterns.values()[id];
				IconButton patternButton = new IconButton(x + background.width - 76 + col * 18, y + 21 + row * 18, pattern.icon);
				patternButton.withCallback(() -> {
					patternButtons.forEach(b -> b.active = true);
					patternButton.active = false;
					currentPattern = pattern;
				});
				patternButton.setToolTip(Lang.translateDirect("gui.terrainzapper.pattern." + pattern.translationKey));
				patternButtons.add(patternButton);
			}
		}

		patternButtons.get(currentPattern.ordinal()).active = false;

		addRenderableWidgets(patternButtons);
	}

	@Override
	protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft;
		int y = guiTop;

		background.render(graphics, x, y);
		drawOnBackground(graphics, x, y);

		renderBlock(graphics, x, y);
		renderZapper(graphics, x, y);
	}

	protected void drawOnBackground(GuiGraphics graphics, int x, int y) {
		graphics.drawString(font, title, x + 11, y + 4, 0x54214F, false);
	}

	@Override
	public void tick() {
		super.tick();
		animationProgress += 5;
	}

	@Override
	public void removed() {
		ConfigureZapperPacket packet = getConfigurationPacket();
		packet.configureZapper(zapper);
		AllPackets.getChannel().sendToServer(packet);
	}

	protected void renderZapper(GuiGraphics graphics, int x, int y) {
		GuiGameElement.of(zapper)
				.scale(4)
				.at(x + background.width, y + background.height - 48, -200)
				.render(graphics);
	}

	@SuppressWarnings("deprecation")
	protected void renderBlock(GuiGraphics graphics, int x, int y) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(x + 32, y + 42, 120);
		ms.mulPose(Axis.XP.rotationDegrees(-25f));
		ms.mulPose(Axis.YP.rotationDegrees(-45f));
		ms.scale(20, 20, 20);

		BlockState state = Blocks.AIR.defaultBlockState();
		if (zapper.has(AllDataComponents.ZAPPER) && zapper.get(AllDataComponents.ZAPPER)
			.contains("BlockUsed"))
			state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), zapper.get(AllDataComponents.ZAPPER)
				.getCompound("BlockUsed"));

		GuiGameElement.of(state)
			.render(graphics);
		ms.popPose();
	}

	protected abstract ConfigureZapperPacket getConfigurationPacket();

}
