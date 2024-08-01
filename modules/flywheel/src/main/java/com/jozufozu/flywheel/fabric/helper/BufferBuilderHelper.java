package com.jozufozu.flywheel.fabric.helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jozufozu.flywheel.fabric.mixin.BufferBuilderAccessor;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

public final class BufferBuilderHelper {
	public static void fixByteOrder(BufferBuilder self, ByteBuffer buffer) {
		buffer.order(/*((BufferBuilderAccessor) self).getBuffer().order()*/ ByteOrder.nativeOrder());
	}

	public static VertexFormat getVertexFormat(BufferBuilder self) {
		return ((BufferBuilderAccessor) self).getFormat();
	}
}
