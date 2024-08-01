package io.github.fabricators_of_create.porting_lib.mixin.common;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import io.github.fabricators_of_create.porting_lib.PortingLibBase;
import io.github.fabricators_of_create.porting_lib.common.util.MixinHelper;
import io.github.fabricators_of_create.porting_lib.extensions.extensions.INBTSerializableCompound;
import io.github.fabricators_of_create.porting_lib.extensions.extensions.ItemStackExtensions;
import io.github.fabricators_of_create.porting_lib.item.DamageableItem;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.fabricators_of_create.porting_lib.item.CustomMaxCountItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements INBTSerializableCompound, ItemStackExtensions {

	@Shadow
	public abstract Tag saveOptional(HolderLookup.Provider levelRegistryAccess);

	@Shadow
	public abstract Item getItem();

	@Shadow
	private PatchedDataComponentMap components;

	@Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
	public void port_lib$onGetMaxCount(CallbackInfoReturnable<Integer> cir) {
		ItemStack self = (ItemStack) (Object) this;
		Item item = self.getItem();
		if (item instanceof CustomMaxCountItem) {
			cir.setReturnValue(((CustomMaxCountItem) item).getItemStackLimit(self));
		}
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = (CompoundTag)this.saveOptional(PortingLibBase.getRegistryAccess());
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		ItemStack stack = ItemStack.parseOptional(PortingLibBase.getRegistryAccess(), nbt);
		this.components = (PatchedDataComponentMap) stack.getComponents();
	}

	@Inject(method = "setDamageValue", at = @At("HEAD"), cancellable = true)
	public void port_lib$itemSetDamage(int damage, CallbackInfo ci) {
		if(getItem() instanceof DamageableItem damagableItem) {
			damagableItem.setDamage((ItemStack) (Object) this, damage);
			ci.cancel();
		}
	}

	@Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
	public void port_lib$itemMaxDamage(CallbackInfoReturnable<Integer> cir) {
		if(getItem() instanceof DamageableItem damagableItem) {
			cir.setReturnValue(damagableItem.getMaxDamage((ItemStack) (Object) this));
		}
	}

	@Inject(method = "getDamageValue", at = @At("HEAD"), cancellable = true)
	public void port_lib$itemDamage(CallbackInfoReturnable<Integer> cir) {
		if(getItem() instanceof DamageableItem damagableItem) {
			cir.setReturnValue(damagableItem.getDamage((ItemStack) (Object) this));
		}
	}
	
	@Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
	public void port_lib$tooltipPartHide(DataComponentType component, Item.TooltipContext context, 
			Consumer tooltipAdder, TooltipFlag tooltipFlag, CallbackInfo cir) {
		int flags = getItem().getDefaultTooltipHideFlags(MixinHelper.cast(this));
		if((flags & PortingLibBase.TooltipPart.ENCHANTMENTS.getMask()) == 1 && 
				(component == DataComponents.ENCHANTMENTS || component == DataComponents.STORED_ENCHANTMENTS)) cir.cancel(); 
		if((flags & PortingLibBase.TooltipPart.UNBREAKABLE.getMask()) == 1 && 
				(component == DataComponents.UNBREAKABLE)) cir.cancel(); 
		if((flags & PortingLibBase.TooltipPart.DYE.getMask()) == 1 && 
				(component == DataComponents.DYED_COLOR)) cir.cancel(); 
		if((flags & PortingLibBase.TooltipPart.UPGRADES.getMask()) == 1 && 
				(component == DataComponents.TRIM)) cir.cancel(); 
	}
	
	@Inject(method = "addModifierTooltip", at = @At("HEAD"), cancellable = true)
	public void port_lib$tooltipModifierHide(Consumer tooltipAdder, @Nullable Player player, 
			Holder attribute, AttributeModifier modfier, CallbackInfo ci) {
		int flags = getItem().getDefaultTooltipHideFlags(MixinHelper.cast(this));
		if((flags & PortingLibBase.TooltipPart.MODIFIERS.getMask()) == 1) ci.cancel(); 
	}
	
	@Inject(method = "parseOptional", at = @At("HEAD"), cancellable = true)
	private static void port_lib$parseOptional(HolderLookup.Provider lookupProvider, CompoundTag tag, CallbackInfoReturnable<ItemStack> clb) {
		if(tag.contains("id") && tag.getString("id").equals("minecraft:air")) {
			LogUtils.getLogger().warn("Problem parsing buggy ItemStack: "+tag);
			clb.setReturnValue(ItemStack.EMPTY);
			clb.cancel();
		}
	}
	
	
}
