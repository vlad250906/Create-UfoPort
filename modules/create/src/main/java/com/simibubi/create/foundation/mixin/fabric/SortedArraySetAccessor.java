package com.simibubi.create.foundation.mixin.fabric;

import java.util.Comparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.util.SortedArraySet;

@Mixin(SortedArraySet.class)
public interface SortedArraySetAccessor<T> {
	@Accessor("contents")
	void create$setContents(T[] contents);

	@Accessor("contents")
	T[] create$getContents();

	@Accessor("comparator")
	Comparator<T> create$getComparator();

	@Accessor("size")
	void create$setSize(int size);

	@Invoker("removeInternal")
	void create$callRemoveInternal(int index);
}
