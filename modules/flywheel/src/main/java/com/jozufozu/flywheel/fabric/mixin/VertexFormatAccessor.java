package com.jozufozu.flywheel.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.vertex.VertexFormat;

import it.unimi.dsi.fastutil.ints.IntList;

@Mixin(VertexFormat.class)
public interface VertexFormatAccessor {
	@Accessor("offsetsByElement")
	int[] getOffsets();
}
