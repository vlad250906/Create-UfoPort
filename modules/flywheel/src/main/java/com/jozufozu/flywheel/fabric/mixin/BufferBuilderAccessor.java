package com.jozufozu.flywheel.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
	@Accessor("buffer")
	ByteBufferBuilder getBuffer();

	@Accessor("format")
	VertexFormat getFormat();
}
