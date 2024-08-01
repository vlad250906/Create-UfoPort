package com.simibubi.create.foundation.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.simibubi.create.Create;

import io.github.fabricators_of_create.porting_lib.models.CompositeModel;
import io.github.fabricators_of_create.porting_lib.models.CompositeModel.Baked;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.WrapperBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class MultiRenderTypeModel extends ForwardingBakedModel {
	protected static final RenderMaterial SOLID_MATERIAL;
	protected static final RenderMaterial CUTOUT_MATERIAL;
	protected static final RenderMaterial TRANSLUCENT_MATERIAL;

	static {
		Renderer renderer = RendererAccess.INSTANCE.getRenderer();
		if (renderer == null) {
			Create.LOGGER.error("No renderer present, rendering will be wrong. If you have Sodium, install Indium!");
			SOLID_MATERIAL = CUTOUT_MATERIAL = TRANSLUCENT_MATERIAL = null;
		} else {
			MaterialFinder finder = renderer.materialFinder();
			SOLID_MATERIAL = finder.blendMode(0, BlendMode.SOLID).find();
			CUTOUT_MATERIAL = finder.blendMode(0, BlendMode.CUTOUT).find();
			TRANSLUCENT_MATERIAL = finder.blendMode(0, BlendMode.TRANSLUCENT).find();
		}
	}

	protected final Map<String, RenderMaterial> parts;

	protected MultiRenderTypeModel(CompositeModel.Baked wrapped, Map<String, RenderMaterial> parts) {
		this.wrapped = wrapped;
		this.parts = parts;
		parts.keySet().forEach(name -> {
			BakedModel part = wrapped.getPart(name);
			if (part == null)
				throw new IllegalArgumentException("Expected part " + name + " is not present");
		});
	}

	public static Builder builder() {
		return new Builder();
	}

	public static BakedModel forNixieTube(BakedModel wrapped) {
		return builder().solid("connectors").translucent("tubes").build(wrapped);
	}

	public static BakedModel forDisplayLink(BakedModel wrapped) {
		return builder().solid("base").cutout("bulb_inner").translucent("bulb").build(wrapped);
	}

	protected FabricBakedModel getPart(String name) {
		return ((Baked) wrapped).getPart(name);
	}

	protected static boolean setMaterial(MutableQuadView quad, RenderMaterial material) {
		quad.material(material);
		return true;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
			Supplier<RandomSource> randomSupplier, RenderContext context) {
		parts.forEach((name, material) -> {
			context.pushTransform(quad -> setMaterial(quad, material));
			getPart(name).emitBlockQuads(blockView, state, pos, randomSupplier, context);
			context.popTransform();
		});
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
		parts.forEach((name, material) -> {
			context.pushTransform(quad -> setMaterial(quad, material));
			getPart(name).emitItemQuads(stack, randomSupplier, context);
			context.popTransform();
		});
	}

	public static boolean isInvalid() {
		return SOLID_MATERIAL == null || TRANSLUCENT_MATERIAL == null || CUTOUT_MATERIAL == null;
	}

	public static class Builder {
		private final Set<String> solidParts = new HashSet<>();
		private final Set<String> translucentParts = new HashSet<>();
		private final Set<String> cutoutParts = new HashSet<>();

		public Builder solid(String... solid) {
			Collections.addAll(solidParts, solid);
			return this;
		}

		public Builder translucent(String... translucent) {
			Collections.addAll(translucentParts, translucent);
			return this;
		}

		public Builder cutout(String... cutout) {
			Collections.addAll(cutoutParts, cutout);
			return this;
		}

		public BakedModel build(BakedModel wrapped) {
			// Sometimes mod's like continuity wrap models, in which case they won't be a
			// CompositeModel.Baked anymore
			// this just unwraps it, so we get the true model.
			while (!(wrapped instanceof Baked baked)) {
				if (wrapped instanceof WrapperBakedModel wrapperModel) {
					wrapped = wrapperModel.getWrappedModel();
				} else {
					throw new IllegalArgumentException(
							"Cannot create a MultiRenderTypeModel for a wrapped model that isn't CompositeModel.Baked");
				}
			}

			if (isInvalid())
				return wrapped;

			Map<String, RenderMaterial> parts = new HashMap<>();
			solidParts.forEach(part -> parts.put(part, SOLID_MATERIAL));
			translucentParts.forEach(part -> parts.put(part, TRANSLUCENT_MATERIAL));
			cutoutParts.forEach(part -> parts.put(part, CUTOUT_MATERIAL));
			return new MultiRenderTypeModel(baked, parts);
		}
	}
}
