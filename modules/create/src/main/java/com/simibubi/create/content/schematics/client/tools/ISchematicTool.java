package com.simibubi.create.content.schematics.client.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

public interface ISchematicTool {

	public void init();
	public void updateSelection();

	public boolean handleRightClick();
	public boolean handleMouseWheel(double delta);
	public void renderTool(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera);
	public void renderOverlay(GuiGraphics graphics, float partialTicks, int width, int height);
	public void renderOnSchematic(PoseStack ms, SuperRenderTypeBuffer buffer);

}
