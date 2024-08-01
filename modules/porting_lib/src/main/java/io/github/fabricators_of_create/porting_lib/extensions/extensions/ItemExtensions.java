package io.github.fabricators_of_create.porting_lib.extensions.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

import io.github.fabricators_of_create.porting_lib.PortingLibBase;

public interface ItemExtensions {
	/**
	 * Called before a block is broken. Return true to prevent default block
	 * harvesting.
	 *
	 * Note: In SMP, this is called on both client and server sides!
	 *
	 * @param itemstack The current ItemStack
	 * @param pos       Block's position in world
	 * @param player    The Player that is wielding the item
	 * @return True to prevent harvesting, false to continue as normal
	 */
	default boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player) {
		return false;
	}

	/**
	 * Called when the player Left Clicks (attacks) an entity. Processed before
	 * damage is done, if return value is true further processing is canceled and
	 * the entity is not attacked.
	 *
	 * @param stack  The Item being used
	 * @param player The player that is attacking
	 * @param entity The entity being attacked
	 * @return True to cancel the rest of the interaction.
	 */
	default boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
		return false;
	}

	/**
	 * Called to get the Mod ID of the mod that *created* the ItemStack, instead of
	 * the real Mod ID that *registered* it.
	 *
	 * For example the Forge Universal Bucket creates a subitem for each modded
	 * fluid, and it returns the modded fluid's Mod ID here.
	 *
	 * Mods that register subitems for other mods can override this. Informational
	 * mods can call it to show the mod that created the item.
	 *
	 * @param itemStack the ItemStack to check
	 * @return the Mod ID for the ItemStack, or null when there is no specially
	 *         associated mod and {@link Registry#getKey(Object)} would return null.
	 */
	@Nullable
	default String getCreatorModId(ItemStack itemStack) {
		Item item = itemStack.getItem();
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
		String modId = registryName == null ? null : registryName.getNamespace();
		if ("minecraft".equals(modId)) {
			if (item instanceof EnchantedBookItem) {
				ItemEnchantments enchs = itemStack.get(DataComponents.STORED_ENCHANTMENTS);
				if(enchs.size() == 1) {
					Holder<Enchantment> holder = enchs.keySet().iterator().next();
					ResourceLocation key = ResourceLocation.tryParse(holder.getRegisteredName());
					if(holder != null && PortingLibBase.getRegistryAccess().lookup(Registries.ENCHANTMENT).get().getOrThrow(holder.unwrapKey().get()) != null) {
						return key.getNamespace();
					}
				}
			} else if (item instanceof PotionItem || item instanceof TippedArrowItem) {
				PotionContents cont = itemStack.get(DataComponents.POTION_CONTENTS);
				Potion potionType = cont.potion().get().value();
				ResourceLocation resourceLocation = BuiltInRegistries.POTION.getKey(potionType);
				if (resourceLocation != null) {
					return resourceLocation.getNamespace();
				}
			} else if (item instanceof SpawnEggItem) {
				ResourceLocation resourceLocation = BuiltInRegistries.ENTITY_TYPE.getKey(((SpawnEggItem) item).getType(null));
				if (resourceLocation != null) {
					return resourceLocation.getNamespace();
				}
			}
		}
		return modId;
	}

	/**
	 * Get the tooltip parts that should be hidden by default on the given stack if the {@code HideFlags} tag is not set.
	 * @see ItemStack.TooltipPart
	 * @param stack the stack
	 * @return the default hide flags
	 */
	default int getDefaultTooltipHideFlags(@Nonnull ItemStack stack) {
		return 0;
	}
}
