package com.simibubi.create.content.trains.schedule;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.mixin.accessor.AgeableListModelAccessor;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.Couple;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.ModelPartAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.FrogModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TrainHatArmorLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

	private Vec3 offset;

	public TrainHatArmorLayer(RenderLayerParent<T, M> renderer, Vec3 offset) {
		super(renderer);
		this.offset = offset;
	}

	@Override
	public void render(PoseStack ms, MultiBufferSource buffer, int light, LivingEntity entity, float yaw, float pitch,
			float pt, float p_225628_8_, float p_225628_9_, float p_225628_10_) {
		if (!shouldRenderOn(entity))
			return;

		M entityModel = getParentModel();
		RenderType renderType = Sheets.cutoutBlockSheet();
		ms.pushPose();

		boolean valid = false;
		TransformStack msr = TransformStack.cast(ms);
		float scale = 1;

		if (entityModel instanceof AgeableListModel<?> model
				&& entityModel instanceof io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.AgeableListModelAccessor access) {
			if (model.young) {
				if (access.porting_lib$scaleHead()) {
					float f = 1.5F / access.porting_lib$babyHeadScale();
					ms.scale(f, f, f);
				}
				ms.translate(0.0D, access.porting_lib$babyYHeadOffset() / 16.0F,
						access.porting_lib$babyZHeadOffset() / 16.0F);
			}

			ModelPart head = getHeadPart(model);
			if (head != null) {
				head.translateAndRotate(ms);

				if (model instanceof WolfModel)
					head = head.getChild("real_head");
				if (model instanceof AxolotlModel)
					head = head.getChild("head");

				ms.translate(offset.x / 16f, offset.y / 16f, offset.z / 16f);

				if (!head.isEmpty()) {
					Cube cube = ((ModelPartAccessor) (Object) head).porting_lib$cubes().get(0);
					ms.translate(offset.x / 16f, (cube.minY - cube.maxY + offset.y) / 16f, offset.z / 16f);
					float max = Math.max(cube.maxX - cube.minX, cube.maxZ - cube.minZ) / 8f;
					ms.scale(max, max, max);
				}

				valid = true;
			}
		}

		else if (entityModel instanceof HierarchicalModel<?> model) {
			boolean slime = model instanceof SlimeModel || model instanceof LavaSlimeModel;
			ModelPart root = model.root();
			String headName = slime ? "cube" : "head";
			ModelPart head = root.hasChild(headName) ? root.getChild(headName) : null;

			if (model instanceof WardenModel)
				head = root.getChild("bone").getChild("body").getChild("head");

			if (model instanceof FrogModel) {
				head = root.getChild("body").getChild("head");
				scale = .5f;
			}

			if (head != null) {
				head.translateAndRotate(ms);

				if (!head.isEmpty()) {
					ModelPartAccessor headAccess = (ModelPartAccessor) (Object) head;
					Cube cube = headAccess.porting_lib$cubes().get(0);
					ms.translate(offset.x, (cube.minY - cube.maxY + offset.y) / 16f, offset.z / 16f);
					float max = Math.max(cube.maxX - cube.minX, cube.maxZ - cube.minZ) / (slime ? 6.5f : 8f) * scale;
					ms.scale(max, max, max);
				}

				valid = true;
			}
		}

		if (valid) {
			ms.scale(1, -1, -1);
			ms.translate(0, -2.25f / 16f, 0);
			msr.rotateX(-8.5f);
			BlockState air = Blocks.AIR.defaultBlockState();
			CachedBufferer.partial(AllPartialModels.TRAIN_HAT, air).forEntityRender().light(light).renderInto(ms,
					buffer.getBuffer(renderType));
		}

		ms.popPose();
	}

	private boolean shouldRenderOn(LivingEntity entity) {
		if (entity == null)
			return false;
		if (entity.getCustomData().contains("TrainHat"))
			return true;
		if (!entity.isPassenger())
			return false;
		if (entity instanceof Player p) {
			ItemStack headItem = p.getItemBySlot(EquipmentSlot.HEAD);
			if (!headItem.isEmpty())
				return false;
		}
		Entity vehicle = entity.getVehicle();
		if (!(vehicle instanceof CarriageContraptionEntity cce))
			return false;
		if (!cce.hasSchedule() && !(entity instanceof Player))
			return false;
		Contraption contraption = cce.getContraption();
		if (!(contraption instanceof CarriageContraption cc))
			return false;
		BlockPos seatOf = cc.getSeatOf(entity.getUUID());
		if (seatOf == null)
			return false;
		Couple<Boolean> validSides = cc.conductorSeats.get(seatOf);
		return validSides != null;
	}

	public static void registerOn(EntityRenderer<?> entityRenderer, RegistrationHelper helper) {
		if (!(entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer))
			return;

		EntityModel<?> model = livingRenderer.getModel();

		if (!(model instanceof HierarchicalModel) && !(model instanceof AgeableListModel))
			return;

		Vec3 offset = TrainHatOffsets.getOffset(model);
		TrainHatArmorLayer<?, ?> layer = new TrainHatArmorLayer<>(livingRenderer, offset);
		helper.register(layer);
	}

	private static ModelPart getHeadPart(AgeableListModel<?> model) {
		for (ModelPart part : ((AgeableListModelAccessor) model).create$callHeadParts())
			return part;
		for (ModelPart part : ((AgeableListModelAccessor) model).create$callBodyParts())
			return part;
		return null;
	}

}
