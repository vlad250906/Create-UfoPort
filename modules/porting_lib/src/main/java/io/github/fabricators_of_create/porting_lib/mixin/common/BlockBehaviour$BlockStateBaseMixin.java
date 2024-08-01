package io.github.fabricators_of_create.porting_lib.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.fabricators_of_create.porting_lib.block.ValidSpawnBlock;
import io.github.fabricators_of_create.porting_lib.item.BlockUseBypassingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockBehaviour$BlockStateBaseMixin {
	
	@Shadow
	protected abstract BlockState asState();
	
	@Inject(at = @At("HEAD"), method = "useItemOn", cancellable = true)
	private void port_lib$use(ItemStack origStack, Level level, Player player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<ItemInteractionResult> cir) {
		Item held = player.getItemInHand(hand).getItem();
		BlockPos pos = result.getBlockPos();
		if (held instanceof BlockUseBypassingItem bypassing) {
			if (bypassing.shouldBypass(level.getBlockState(pos), pos, level, player, hand)) cir.setReturnValue(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION);
		} else if (held instanceof BlockItem blockItem && blockItem.getBlock() instanceof BlockUseBypassingItem bypassing) {
			if (bypassing.shouldBypass(level.getBlockState(pos), pos, level, player, hand)) cir.setReturnValue(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION);
		}
	}
	
	@ModifyReturnValue(
			method = "isValidSpawn",
			at = @At("RETURN")
	)
	private boolean port_lib$validSpawnBlock(boolean original, @Local BlockGetter getter, @Local BlockPos pos, @Local EntityType type) {
		BlockState state = asState();
		if (state.getBlock() instanceof ValidSpawnBlock validSpawnBlock)
			return validSpawnBlock.isValidSpawn(state, getter, pos, SpawnPlacementTypes.ON_GROUND, type);
		return original;
	}
	
}
