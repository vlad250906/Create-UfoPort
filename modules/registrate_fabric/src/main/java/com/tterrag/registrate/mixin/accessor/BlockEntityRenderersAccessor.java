package com.tterrag.registrate.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

@Mixin(BlockEntityRenderers.class)
public interface BlockEntityRenderersAccessor {
	@Invoker("register")
	static <T extends BlockEntity> void invokeRegister(BlockEntityType<? extends T> blockEntityType,
			BlockEntityRendererProvider<T> blockEntityRendererProvider) {
		throw new RuntimeException("Mixin not applied!");
	}
}
