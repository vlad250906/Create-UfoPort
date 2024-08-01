package com.jozufozu.flywheel.mixin.instancemanage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(SectionCompiler.class)
public abstract class ChunkRebuildHooksMixin {
	
	@Redirect(
			method = "handleBlockEntity", 
			at = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"
			)
	)
	private BlockEntityRenderer flywheel$tryAddBlockEntity(BlockEntityRenderDispatcher inst, BlockEntity blockEntity) {
		if (InstancedRenderDispatcher.tryAddBlockEntity(blockEntity)) {
			return null;
		}
		return inst.getRenderer(blockEntity);
	}
}
