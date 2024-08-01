package com.jozufozu.flywheel.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.core.crumbling.CrumblingRenderer;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.jozufozu.flywheel.event.ReloadRenderersEvent;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.jozufozu.flywheel.fabric.event.FlywheelEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;

@Mixin(value = LevelRenderer.class, priority = 1001) // Higher priority to go after sodium
public class LevelRendererMixin {
	@Shadow
	private ClientLevel level;

	@Shadow
	@Final
	private RenderBuffers renderBuffers;
	
	@Unique
	private Camera camera;

	@Inject(at = @At("HEAD"), method = "setupRender")
	private void setupRender(Camera camera, Frustum frustum, boolean queue, boolean isSpectator, CallbackInfo ci) {
		this.camera = camera;
		FlywheelEvents.BEGIN_FRAME.invoker().handleEvent(new BeginFrameEvent(level, camera, frustum));
	}

	@Inject(at = @At("TAIL"), method = "renderSectionLayer")
	private void renderLayer(RenderType type, double camX, double camY, double camZ, Matrix4f frustum,
			Matrix4f projection, CallbackInfo ci) {
		
		PoseStack ps = new PoseStack();
		ps.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		ps.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0f));
		
		FlywheelEvents.RENDER_LAYER.invoker()
				.handleEvent(new RenderLayerEvent(level, type, ps, renderBuffers, camX, camY, camZ));
	}

	@Inject(at = @At("TAIL"), method = "allChanged")
	private void refresh(CallbackInfo ci) {
		Backend.refresh();

		FlywheelEvents.RELOAD_RENDERERS.invoker().handleEvent(new ReloadRenderersEvent(level));
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 2 // after
																																									// the
																																									// game
																																									// renders
																																									// the
																																									// breaking
																																									// overlay
																																									// normally
	), method = "renderLevel")
	private void renderBlockBreaking(DeltaTracker tracker, boolean flag,
			Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustum, Matrix4f p_228426_9_,
			CallbackInfo ci) {
		PoseStack ps = new PoseStack();
		ps.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		ps.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0f));
		CrumblingRenderer.render(level, camera, ps);
	}
}
