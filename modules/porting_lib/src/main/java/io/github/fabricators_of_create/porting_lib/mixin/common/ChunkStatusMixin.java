package io.github.fabricators_of_create.porting_lib.mixin.common;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.level.chunk.status.ChunkStatus;


@Mixin(ChunkStatus.class)
public abstract class ChunkStatusMixin {
	@Shadow
	@Final
	@Mutable
	public static ChunkStatus FULL;
}
