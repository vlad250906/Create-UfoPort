package io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.platform.InputConstants.Key;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;

@Environment(EnvType.CLIENT)
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
	@Accessor("key")
	Key port_lib$getKey();
}
