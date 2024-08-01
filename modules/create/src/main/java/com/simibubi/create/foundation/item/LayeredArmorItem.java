package com.simibubi.create.foundation.item;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.mixin.accessor.HumanoidArmorLayerAccessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public interface LayeredArmorItem extends CustomRenderedArmorItem {
	@Environment(EnvType.CLIENT)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	default void renderArmorPiece(HumanoidArmorLayer<?, ?, ?> layer, PoseStack poseStack,
			MultiBufferSource bufferSource, LivingEntity entity, EquipmentSlot slot, int light,
			HumanoidModel<?> originalModel, ItemStack stack) {
		if (!(stack.getItem() instanceof ArmorItem item)) {
			return;
		}
		if (entity.getEquipmentSlotForItem(stack) != slot) {
			return;
		}

		HumanoidArmorLayerAccessor accessor = (HumanoidArmorLayerAccessor) layer;
		Map<String, ResourceLocation> locationCache = HumanoidArmorLayerAccessor.create$getArmorLocationCache();
		boolean glint = stack.hasFoil();

		HumanoidModel<?> innerModel = accessor.create$getInnerModel();
		layer.getParentModel().copyPropertiesTo((HumanoidModel) innerModel);
		accessor.create$callSetPartVisibility(innerModel, slot);
		String locationStr2 = getArmorTextureLocation(entity, slot, stack, 2);
		ResourceLocation location2 = locationCache.computeIfAbsent(locationStr2, ResourceLocation::parse);
		renderModel(poseStack, bufferSource, light, item, innerModel, glint, 1.0F, 1.0F, 1.0F, location2);

		HumanoidModel<?> outerModel = accessor.create$getOuterModel();
		layer.getParentModel().copyPropertiesTo((HumanoidModel) outerModel);
		accessor.create$callSetPartVisibility(outerModel, slot);
		String locationStr1 = getArmorTextureLocation(entity, slot, stack, 1);
		ResourceLocation location1 = locationCache.computeIfAbsent(locationStr1, ResourceLocation::parse);
		renderModel(poseStack, bufferSource, light, item, outerModel, glint, 1.0F, 1.0F, 1.0F, location1);
	}

	// from HumanoidArmorLayer.renderModel
	private void renderModel(PoseStack poseStack, MultiBufferSource bufferSource, int light, ArmorItem item,
		Model model, boolean glint, float red, float green, float blue, ResourceLocation armorResource) {
		VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.armorCutoutNoCull(armorResource));
		model.renderToBuffer(poseStack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, ARGB32.colorFromFloat(1.0f, red, blue, green));
	}

	String getArmorTextureLocation(LivingEntity entity, EquipmentSlot slot, ItemStack stack, int layer);
}
