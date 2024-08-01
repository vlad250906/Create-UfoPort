package com.simibubi.create.foundation.item.render;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.fabricators_of_create.porting_lib.models.TransformTypeDependentItemBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;

public class CustomRenderedItemModel extends ForwardingBakedModel implements TransformTypeDependentItemBakedModel {

	public CustomRenderedItemModel(BakedModel originalModel) {
		this.wrapped = originalModel;
	}

	@Override
	public boolean isCustomRenderer() {
		return true;
	}

	@Override
	public BakedModel applyTransform(ItemDisplayContext cameraItemDisplayContext, PoseStack mat,
									 boolean leftHand, DefaultTransform defaultTransform) {
		// fabric: apply the wrapped model transforms, but render this model
		defaultTransform.apply(wrapped);
		return this;
	}

	public BakedModel getOriginalModel() {
		return wrapped;
	}

}
