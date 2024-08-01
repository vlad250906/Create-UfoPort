package io.github.fabricators_of_create.porting_lib.extensions.extensions;

import io.github.fabricators_of_create.porting_lib.core.util.INBTSerializable;
import net.minecraft.nbt.CompoundTag;

import org.jetbrains.annotations.ApiStatus;

/**
 * This class exists since we can't use generics for injection. Use
 * {@link INBTSerializable <CompoundTag>} instead.
 */
@ApiStatus.Internal
public interface INBTSerializableCompound extends INBTSerializable<CompoundTag> {
}
