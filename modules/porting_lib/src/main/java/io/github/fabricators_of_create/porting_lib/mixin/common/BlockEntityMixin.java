package io.github.fabricators_of_create.porting_lib.mixin.common;

import io.github.fabricators_of_create.porting_lib.extensions.extensions.INBTSerializableCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.fabricators_of_create.porting_lib.PortingLibBase;
import io.github.fabricators_of_create.porting_lib.extensions.extensions.BlockEntityExtensions;
import io.github.fabricators_of_create.porting_lib.util.BlockEntityHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements BlockEntityExtensions, INBTSerializableCompound {
	@Unique
	private CompoundTag port_lib$extraData = null;

	@Shadow
	public abstract void loadAdditional(CompoundTag tag, HolderLookup.Provider registries);

	@Shadow
	public abstract CompoundTag saveWithFullMetadata(HolderLookup.Provider registries);

	@Inject(at = @At("RETURN"), method = "saveMetadata")
	private void port_lib$saveMetadata(CompoundTag nbt, CallbackInfo ci) {
		if (port_lib$extraData != null && !port_lib$extraData.isEmpty()) {
			nbt.put(BlockEntityHelper.EXTRA_DATA_KEY, port_lib$extraData);
		}
	}

	@Inject(at = @At("RETURN"), method = "loadAdditional")
	private void port_lib$load(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
		if (tag.contains(BlockEntityHelper.EXTRA_DATA_KEY)) {
			port_lib$extraData = tag.getCompound(BlockEntityHelper.EXTRA_DATA_KEY);
		}
	}

	@Inject(method = "setRemoved", at = @At("TAIL"))
	public void port_lib$invalidate(CallbackInfo ci) {
		invalidateCaps();
	}

	@Override
	public CompoundTag serializeNBT() {
		return this.saveWithFullMetadata(PortingLibBase.getRegistryAccess());
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		deserializeNBT(null, nbt);
	}

	@Override
	public CompoundTag getCustomData() {
		if (port_lib$extraData == null) {
			port_lib$extraData = new CompoundTag();
		}
		return port_lib$extraData;
	}

	public void deserializeNBT(BlockState state, CompoundTag nbt) {
		this.loadAdditional(nbt, PortingLibBase.getRegistryAccess());
	}
}
