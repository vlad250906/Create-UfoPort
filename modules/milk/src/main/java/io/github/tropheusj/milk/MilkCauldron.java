package io.github.tropheusj.milk;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import javax.swing.text.html.BlockView;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteraction.InteractionMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;

public class MilkCauldron extends LayeredCauldronBlock {
	static final InteractionMap MILK_CAULDRON_BEHAVIOR = CauldronInteraction
			.newInteractionMap("MilkCauldronInteractionMap");
	static final CauldronInteraction FILL_FROM_BUCKET = (state, world, pos, player, hand, stack) -> CauldronInteraction
			.emptyBucket(world, pos, player, hand, stack, Milk.MILK_CAULDRON.defaultBlockState().setValue(LEVEL, 3),
					SoundEvents.BUCKET_EMPTY);
	static final CauldronInteraction EMPTY_TO_BUCKET = (state, world, pos, player, hand, stack) -> CauldronInteraction
			.fillBucket(state, world, pos, player, hand, stack, new ItemStack(Items.MILK_BUCKET),
					statex -> statex.getValue(LEVEL) == 3, SoundEvents.BUCKET_FILL);
	static final CauldronInteraction MILKIFY_DYEABLE_ITEM = (state, world, pos, player, hand, stack) -> {
		Item item = stack.getItem();
		if (stack.has(DataComponents.DYED_COLOR)) {
			if (!world.isClientSide()) {
				stack.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFFFFFF, false));
				//dyeableItem.setColor(stack, 0xFFFFFF);
				player.awardStat(Stats.CLEAN_ARMOR);
				LayeredCauldronBlock.lowerFillLevel(state, world, pos);
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide());
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	};
	static final CauldronInteraction MILKIFY_SHULKER_BOX = (state, world, pos, player, hand, stack) -> {
		Block block = Block.byItem(stack.getItem());
		if ((block instanceof ShulkerBoxBlock)) {
			if (!world.isClientSide()) {
				ItemStack itemStack = new ItemStack(Blocks.WHITE_SHULKER_BOX);
				itemStack.applyComponents(stack.getComponents());
				player.setItemInHand(hand, itemStack);
				player.awardStat(Stats.CLEAN_SHULKER_BOX);
				LayeredCauldronBlock.lowerFillLevel(state, world, pos);
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide());
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	};
	static final CauldronInteraction MILKIFY_BANNER = (state, world, pos, player, hand, stack) -> {
		if (!world.isClientSide()) {
			ItemStack itemStack = new ItemStack(Items.WHITE_BANNER);
			if (!player.isCreative()) {
				stack.shrink(1);
			}

			if (stack.isEmpty()) {
				player.setItemInHand(hand, itemStack);
			} else if (player.getInventory().add(itemStack)) {
				player.inventoryMenu.sendAllDataToRemote();
			} else {
				player.drop(itemStack, false);
			}
			player.awardStat(Stats.CLEAN_BANNER);
			LayeredCauldronBlock.lowerFillLevel(state, world, pos);
		}
		return ItemInteractionResult.sidedSuccess(world.isClientSide());
	};

	public MilkCauldron(BlockBehaviour.Properties properties) {
		super(Biome.Precipitation.NONE, getMilkCauldronBehaviors(), properties);
	}

	private static InteractionMap getMilkCauldronBehaviors() {
		// dyeables
		MILK_CAULDRON_BEHAVIOR.map().put(Items.LEATHER_BOOTS, MilkCauldron.MILKIFY_DYEABLE_ITEM);
		MILK_CAULDRON_BEHAVIOR.map().put(Items.LEATHER_LEGGINGS, MilkCauldron.MILKIFY_DYEABLE_ITEM);
		MILK_CAULDRON_BEHAVIOR.map().put(Items.LEATHER_CHESTPLATE, MilkCauldron.MILKIFY_DYEABLE_ITEM);
		MILK_CAULDRON_BEHAVIOR.map().put(Items.LEATHER_HELMET, MilkCauldron.MILKIFY_DYEABLE_ITEM);
		MILK_CAULDRON_BEHAVIOR.map().put(Items.LEATHER_HORSE_ARMOR, MilkCauldron.MILKIFY_DYEABLE_ITEM);
		MILK_CAULDRON_BEHAVIOR.map().put(Items.WOLF_ARMOR, MilkCauldron.MILKIFY_DYEABLE_ITEM);
		for (Field field : Items.class.getDeclaredFields()) {
			try {
				if (Modifier.isStatic(field.getModifiers())) {
					Object obj = field.get(null);
					if (obj instanceof Item item) {
						if (item instanceof BannerItem) {
							MILK_CAULDRON_BEHAVIOR.map().put(item, MilkCauldron.MILKIFY_BANNER);
						} else if (item instanceof BlockItem blockItem) {
							if (blockItem.getBlock() instanceof ShulkerBoxBlock) {
								MILK_CAULDRON_BEHAVIOR.map().put(item, MilkCauldron.MILKIFY_SHULKER_BOX);
							}
						}
					}
				}
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		MILK_CAULDRON_BEHAVIOR.map().put(Items.MILK_BUCKET, FILL_FROM_BUCKET);
		MILK_CAULDRON_BEHAVIOR.map().put(Items.BUCKET, EMPTY_TO_BUCKET);

		return MILK_CAULDRON_BEHAVIOR;
	}

	@Override
	protected boolean canReceiveStalactiteDrip(Fluid fluid) {
		return fluid instanceof MilkFluid;
	}

	@Override
	public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
		if (!world.isClientSide() && isEntityInsideContent(state, pos, entity) && entity.mayInteract(world, pos)) {
			boolean shouldDrain = false;
			if (entity.isOnFire()) {
				entity.extinguishFire();
				shouldDrain = true;
			}

			if (entity instanceof LivingEntity livingEntity) {
				shouldDrain = Milk.tryRemoveRandomEffect(livingEntity);
			}

			if (shouldDrain) {
				lowerFillLevel(state, world, pos);
			}
		}
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
		return Items.CAULDRON.getDefaultInstance();
	}

	public static CauldronInteraction addBehavior(CauldronInteraction behavior, Item... items) {
		for (Item item : items) {
			MILK_CAULDRON_BEHAVIOR.map().put(item, behavior);
		}
		return behavior;
	}

	public static CauldronInteraction addInputToCauldronExchange(ItemStack toEmpty, ItemStack emptied,
			boolean ignoreNbt) {
		Item emptyItem = toEmpty.getItem();
		CauldronInteraction b = addBehavior(new InputToCauldronCauldronBehavior(toEmpty, emptied, ignoreNbt),
				emptyItem);
		CauldronInteraction.EMPTY.map().put(emptyItem, b);
		return b;
	}

	public static CauldronInteraction addOutputToItemExchange(ItemStack toFill, ItemStack filled, boolean ignoreNbt) {
		return addBehavior(new OutputToItemCauldronBehavior(toFill, filled, ignoreNbt), toFill.getItem());
	}

	private static boolean typeAndDataEqual(ItemStack stack1, ItemStack stack2, boolean ignoreNbt) {
		boolean itemsEqual = ItemStack.matches(stack1, stack2);
		if (ignoreNbt)
			return itemsEqual;
		return ItemStack.matches(stack1, stack2);
	}

	public record OutputToItemCauldronBehavior(ItemStack toFill, ItemStack filled, boolean ignoreNbt)
			implements CauldronInteraction {
		@Override
		public ItemInteractionResult interact(BlockState state, Level world, BlockPos pos, Player player,
				InteractionHand hand, ItemStack held) {
			if (!world.isClientSide() && typeAndDataEqual(held, toFill, ignoreNbt)) {
				Item item = held.getItem();
				player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled.copy()));
				player.awardStat(Stats.USE_CAULDRON);
				player.awardStat(Stats.ITEM_USED.get(item));
				LayeredCauldronBlock.lowerFillLevel(state, world, pos);
				world.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
				world.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide());
		}
	}

	public record InputToCauldronCauldronBehavior(ItemStack toEmpty, ItemStack emptied, boolean ignoreNbt)
			implements CauldronInteraction {
		@Override
		public ItemInteractionResult interact(BlockState state, Level world, BlockPos pos, Player player,
				InteractionHand hand, ItemStack stack) {
			Block block = state.getBlock();
			if ((block == Blocks.CAULDRON || block == Milk.MILK_CAULDRON)
					&& (!state.hasProperty(LEVEL) || state.getValue(LEVEL) != 3)
					&& typeAndDataEqual(stack, toEmpty, ignoreNbt)) {
				if (!world.isClientSide()) {
					player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, emptied.copy()));
					player.awardStat(Stats.USE_CAULDRON);
					player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
					if (block == Blocks.CAULDRON) {
						world.setBlockAndUpdate(pos, Milk.MILK_CAULDRON.defaultBlockState().setValue(LEVEL, 1));
					} else {
						world.setBlockAndUpdate(pos, state.setValue(LEVEL, state.getValue(LEVEL) + 1));
					}
					world.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
					world.gameEvent(null, GameEvent.FLUID_PLACE, pos);
				}
				return ItemInteractionResult.sidedSuccess(world.isClientSide());
			}
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
	}
}
