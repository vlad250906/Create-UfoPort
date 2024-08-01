package io.github.tropheusj.milk.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.tropheusj.milk.Milk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(BottleItem.class)
public abstract class GlassBottleItemMixin extends Item {
	private GlassBottleItemMixin(Properties settings) {
		super(settings);
	}

	@Shadow
	protected abstract ItemStack turnBottleIntoItem(ItemStack itemStack, Player playerEntity, ItemStack itemStack2);

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
			ordinal = 1, shift = At.Shift.AFTER), method = "use", cancellable = true)
	public void milk$use(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
		if (Milk.MILK_BOTTLE != null && Milk.STILL_MILK != null) {
			BlockHitResult hitResult = Item.getPlayerPOVHitResult(world, user, ClipContext.Fluid.SOURCE_ONLY);
			BlockPos blockPos = hitResult.getBlockPos();
			FluidState state = world.getFluidState(blockPos);
			if (Milk.isMilk(state)) {
				cir.setReturnValue(InteractionResultHolder.success(turnBottleIntoItem(user.getItemInHand(hand), user, new ItemStack(Milk.MILK_BOTTLE))));
			}
		}
	}
}
