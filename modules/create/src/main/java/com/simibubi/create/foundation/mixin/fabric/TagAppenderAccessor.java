package com.simibubi.create.foundation.mixin.fabric;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.data.tags.TagsProvider.TagAppender;
import net.minecraft.tags.TagBuilder;

@Mixin(TagAppender.class)
public interface TagAppenderAccessor {
	@Accessor
	TagBuilder getBuilder();
}
