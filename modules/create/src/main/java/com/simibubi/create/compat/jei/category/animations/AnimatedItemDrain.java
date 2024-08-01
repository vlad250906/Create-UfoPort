package com.simibubi.create.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.gui.UIRenderHelper;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;

public class AnimatedItemDrain extends AnimatedKinetics {

	private FluidStack fluid;

	public AnimatedItemDrain withFluid(FluidStack fluid) {
		this.fluid = fluid;
		return this;
	}

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack matrixStack = graphics.pose();
		matrixStack.pushPose();
		matrixStack.translate(xOffset, yOffset, 100);
		matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
		int scale = 20;

		blockElement(AllBlocks.ITEM_DRAIN.getDefaultState())
			.scale(scale)
			.render(graphics);

		BufferSource buffer = MultiBufferSource.immediate(new ByteBufferBuilder(256));
		UIRenderHelper.flipForGuiRender(matrixStack);
		matrixStack.scale(scale, scale, scale);
		float from = 2/16f;
		float to = 1f - from;
		FluidRenderer.renderFluidBox(fluid, from, from, from, to, 3/4f, to, buffer, matrixStack, LightTexture.FULL_BRIGHT, false);
		buffer.endBatch();

		matrixStack.popPose();
	}
}
