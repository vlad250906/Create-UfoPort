package com.jozufozu.flywheel.core.model;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class ShadeSeparatingVertexConsumer implements VertexConsumer {
	private final ShadeSeparatingBakedModel modelWrapper = new ShadeSeparatingBakedModel();
	protected VertexConsumer shadedConsumer;
	protected VertexConsumer unshadedConsumer;
	protected VertexConsumer activeConsumer;

	public void prepare(VertexConsumer shadedConsumer, VertexConsumer unshadedConsumer) {
		this.shadedConsumer = shadedConsumer;
		this.unshadedConsumer = unshadedConsumer;
	}

	public void clear() {
		shadedConsumer = null;
		unshadedConsumer = null;
		activeConsumer = null;
	}

	public BakedModel wrapModel(BakedModel model) {
		modelWrapper.setWrapped(model);
		return modelWrapper;
	}

	protected void setActiveConsumer(boolean shaded) {
		activeConsumer = shaded ? shadedConsumer : unshadedConsumer;
	}

	@Override
	public void putBulkData(PoseStack.Pose poseEntry, BakedQuad quad, float[] colorMuls, float red, float green, float blue, float alpha, int[] combinedLights, int combinedOverlay, boolean mulColor) {
		if (quad.isShade()) {
			shadedConsumer.putBulkData(poseEntry, quad, colorMuls, red, green, blue, alpha, combinedLights, combinedOverlay, mulColor);
		} else {
			unshadedConsumer.putBulkData(poseEntry, quad, colorMuls, red, green, blue, alpha, combinedLights, combinedOverlay, mulColor);
		}
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		activeConsumer.addVertex(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int alpha) {
		activeConsumer.setColor(red, green, blue, alpha);
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		activeConsumer.setUv(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		activeConsumer.setUv1(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		activeConsumer.setUv2(u, v);
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		activeConsumer.setNormal(x, y, z);
		return this;
	}

//	@Override
//	public void endVertex() {
//		activeConsumer.endVertex();
//	}
//
//	@Override
//	public void defaultColor(int red, int green, int blue, int alpha) {
//		activeConsumer.defaultColor(red, green, blue, alpha);
//	}
//
//	@Override
//	public void unsetDefaultColor() {
//		activeConsumer.unsetDefaultColor();
//	}

	private class ShadeSeparatingBakedModel extends ForwardingBakedModel {
		private final QuadTransform quadTransform = quad -> {
			boolean shade = !quad.material().disableDiffuse();
			ShadeSeparatingVertexConsumer.this.setActiveConsumer(shade);
			return true;
		};

		private void setWrapped(BakedModel model) {
			wrapped = model;
		}

		@Override
		public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
			context.pushTransform(quadTransform);
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			context.popTransform();
		}
	}
}
