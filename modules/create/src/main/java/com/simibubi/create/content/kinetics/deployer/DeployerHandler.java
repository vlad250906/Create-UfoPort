package com.simibubi.create.content.kinetics.deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockItem;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity.Mode;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.worldWrappers.WrappedWorld;

import io.github.fabricators_of_create.porting_lib.item.UseFirstBehaviorItem;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.BucketItemAccessor;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class DeployerHandler {

	private static final class ItemUseWorld extends WrappedWorld {
		private final Direction face;
		private final BlockPos pos;
		boolean rayMode = false;

		private ItemUseWorld(Level world, Direction face, BlockPos pos) {
			super(world);
			this.face = face;
			this.pos = pos;
		}

		@Override
		public BlockHitResult clip(ClipContext context) {
			rayMode = true;
			BlockHitResult rayTraceBlocks = super.clip(context);
			rayMode = false;
			return rayTraceBlocks;
		}

		@Override
		public BlockState getBlockState(BlockPos position) {
			if (rayMode && (pos.relative(face.getOpposite(), 3)
				.equals(position)
				|| pos.relative(face.getOpposite(), 1)
					.equals(position)))
				return Blocks.BEDROCK.defaultBlockState();
			return world.getBlockState(position);
		}
	}

	static boolean shouldActivate(ItemStack held, Level world, BlockPos targetPos, @Nullable Direction facing) {
		if (held.getItem() instanceof BlockItem)
			if (world.getBlockState(targetPos)
				.getBlock() == ((BlockItem) held.getItem()).getBlock())
				return false;

		if (held.getItem() instanceof BucketItem) {
			BucketItem bucketItem = (BucketItem) held.getItem();
			Fluid fluid = ((BucketItemAccessor) bucketItem).port_lib$getContent();
			if (fluid != Fluids.EMPTY && world.getFluidState(targetPos)
				.getType() == fluid)
				return false;
		}

		if (!held.isEmpty() && facing == Direction.DOWN
			&& BlockEntityBehaviour.get(world, targetPos, TransportedItemStackHandlerBehaviour.TYPE) != null)
			return false;

		return true;
	}

	static void activate(DeployerFakePlayer player, Vec3 vec, BlockPos clickedPos, Vec3 extensionVector, Mode mode) {
		Multimap<Holder<Attribute>, AttributeModifier> attributeModifiers = HashMultimap.create();
		if(player.getMainHandItem().get(DataComponents.ATTRIBUTE_MODIFIERS) != null)
			player.getMainHandItem().get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers()
				.forEach(ent -> attributeModifiers.put(ent.attribute(), ent.modifier()));
		addTransientAttributeModifiers(player.getAttributes(), attributeModifiers);
		activateInner(player, vec, clickedPos, extensionVector, mode);
		addTransientAttributeModifiers(player.getAttributes(), attributeModifiers);
	}
	
	 public static void addTransientAttributeModifiers(AttributeMap attrs, Multimap<Holder<Attribute>, AttributeModifier> map) {
	        map.forEach((attribute, attributeModifier) -> {
	            AttributeInstance attributeInstance = attrs.getInstance(attribute);
	            if (attributeInstance != null) {
	                attributeInstance.removeModifier(attributeModifier.id());
	                attributeInstance.addTransientModifier((AttributeModifier)attributeModifier);
	            }
	        });
	    }

	private static void activateInner(DeployerFakePlayer player, Vec3 vec, BlockPos clickedPos, Vec3 extensionVector,
		Mode mode) {

		Vec3 rayOrigin = vec.add(extensionVector.scale(3 / 2f + 1 / 64f));
		Vec3 rayTarget = vec.add(extensionVector.scale(5 / 2f - 1 / 64f));
		player.setPos(rayOrigin.x, rayOrigin.y, rayOrigin.z);
		BlockPos pos = BlockPos.containing(vec);
		ItemStack stack = player.getMainHandItem();
		Item item = stack.getItem();

		// Check for entities
		final Level world = player.level();
		List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(clickedPos))
			.stream()
			.filter(e -> !(e instanceof AbstractContraptionEntity))
			.collect(Collectors.toList());
		InteractionHand hand = InteractionHand.MAIN_HAND;
		if (!entities.isEmpty()) {
			Entity entity = entities.get(world.random.nextInt(entities.size()));
			List<ItemEntity> capturedDrops = new ArrayList<>();
			boolean success = false;
			entity.captureDrops(capturedDrops);

			// Use on entity
			if (mode == Mode.USE) {
				InteractionResult cancelResult = UseEntityCallback.EVENT.invoker().interact(player, world, hand, entity, new EntityHitResult(entity));
				if (cancelResult == InteractionResult.FAIL) {
					entity.captureDrops(null);
					return;
				}
				if (cancelResult == null || cancelResult == InteractionResult.PASS) {
					if (entity.interact(player, hand)
						.consumesAction()) {
						if (entity instanceof AbstractVillager) {
							AbstractVillager villager = ((AbstractVillager) entity);
							if (villager.getTradingPlayer() instanceof DeployerFakePlayer)
								villager.setTradingPlayer(null);
						}
						success = true;
					} else if (entity instanceof LivingEntity
						&& stack.interactLivingEntity(player, (LivingEntity) entity, hand)
							.consumesAction())
						success = true;
				}
				if (!success && entity instanceof Player playerEntity) {
					if (stack.has(DataComponents.FOOD)) {
						FoodProperties foodProperties = item.components().get(DataComponents.FOOD);
						if (playerEntity.canEat(foodProperties.canAlwaysEat())) {
							playerEntity.eat(world, stack);
							player.spawnedItemEffects = stack.copy();
							success = true;
						}
					}
					if (AllItemTags.DEPLOYABLE_DRINK.matches(stack)) {
						player.spawnedItemEffects = stack.copy();
						player.setItemInHand(hand, stack.finishUsingItem(world, playerEntity));
						success = true;
					}
				}
			}

			// Punch entity
			if (mode == Mode.PUNCH) {
				player.resetAttackStrengthTicker();
				player.attack(entity);
				success = true;
			}

			entity.captureDrops(null);
			capturedDrops.forEach(e -> player.getInventory()
					.placeItemBackInInventory(e.getItem()));
			if (success)
				return;
		}

		// Shoot ray
		ClipContext rayTraceContext =
			new ClipContext(rayOrigin, rayTarget, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
		BlockHitResult result = world.clip(rayTraceContext);
		if (result.getBlockPos() != clickedPos)
			result = new BlockHitResult(result.getLocation(), result.getDirection(), clickedPos, result.isInside());
		BlockState clickedState = world.getBlockState(clickedPos);
		Direction face = result.getDirection();
		if (face == null)
			face = Direction.getNearest(extensionVector.x, extensionVector.y, extensionVector.z)
				.getOpposite();

		// Left click
		if (mode == Mode.PUNCH) {
			if (!world.mayInteract(player, clickedPos))
				return;
			if (clickedState.getShape(world, clickedPos)
				.isEmpty()) {
				player.blockBreakingProgress = null;
				return;
			}
			InteractionResult actionResult = UseBlockCallback.EVENT.invoker().interact(player, player.level(), player.getUsedItemHand(), result);
			if (actionResult == InteractionResult.FAIL)
				return;
			if (BlockHelper.extinguishFire(world, player, clickedPos, face))
				return;
//			if (actionResult != InteractionResult.FAIL) // fabric: checked above
			clickedState.attack(world, clickedPos, player);
			if (stack.isEmpty())
				return;

			float progress = clickedState.getDestroyProgress(player, world, clickedPos) * 16;
			float before = 0;
			Pair<BlockPos, Float> blockBreakingProgress = player.blockBreakingProgress;
			if (blockBreakingProgress != null)
				before = blockBreakingProgress.getValue();
			progress += before;
			world.playSound(null, clickedPos, clickedState.getSoundType()
				.getHitSound(), SoundSource.NEUTRAL, .25f, 1);

			if (progress >= 1) {
				tryHarvestBlock(player, player.gameMode, clickedPos);
				world.destroyBlockProgress(player.getId(), clickedPos, -1);
				player.blockBreakingProgress = null;
				return;
			}
			if (progress <= 0) {
				player.blockBreakingProgress = null;
				return;
			}

			if ((int) (before * 10) != (int) (progress * 10))
				world.destroyBlockProgress(player.getId(), clickedPos, (int) (progress * 10));
			player.blockBreakingProgress = Pair.of(clickedPos, progress);
			return;
		}

		// Right click
		UseOnContext itemusecontext = new UseOnContext(player, hand, result);
		InteractionResult useBlock = InteractionResult.PASS;
		InteractionResult useItem = InteractionResult.PASS;
		if (!clickedState.getShape(world, clickedPos)
			.isEmpty()) {
			useBlock = UseBlockCallback.EVENT.invoker().interact(player, player.level(), hand, result);
			useItem = useBlock;
		}

		// Item has custom active use
		if (useItem != InteractionResult.FAIL && stack.getItem() instanceof UseFirstBehaviorItem first) {
			InteractionResult actionresult = first.onItemUseFirst(stack, itemusecontext);
			if (actionresult != InteractionResult.PASS)
				return;
		}

		boolean holdingSomething = !player.getMainHandItem()
			.isEmpty();
		boolean flag1 =
			!(player.isShiftKeyDown() && holdingSomething)/* || (stack.doesSneakBypassUse(world, clickedPos, player))*/;

		// Use on block
		if (useBlock != null && flag1
			&& safeOnUse(clickedState, world, clickedPos, player, hand, result).consumesAction())
			return;
		if (stack.isEmpty())
			return;
		if (useItem == null)
			return;

		// Reposition fire placement for convenience
		if (item == Items.FLINT_AND_STEEL) {
			Direction newFace = result.getDirection();
			BlockPos newPos = result.getBlockPos();
			if (!BaseFireBlock.canBePlacedAt(world, clickedPos, newFace))
				newFace = Direction.UP;
			if (clickedState.isAir())
				newPos = newPos.relative(face.getOpposite());
			result = new BlockHitResult(result.getLocation(), newFace, newPos, result.isInside());
			itemusecontext = new UseOnContext(player, hand, result);
		}

		// 'Inert' item use behaviour & block placement
		InteractionResult onItemUse = stack.useOn(itemusecontext);
		if (onItemUse.consumesAction()) {
			if (stack.getItem() instanceof BlockItem bi
				&& (bi.getBlock() instanceof BaseRailBlock || bi.getBlock() instanceof ITrackBlock))
				player.placedTracks = true;
			return;
		}

		if (item instanceof BlockItem && !(item instanceof CartAssemblerBlockItem)
				&& !clickedState.canBeReplaced(new BlockPlaceContext(itemusecontext)))
			return;
		if (item == Items.ENDER_PEARL)
			return;
		if (AllItemTags.DEPLOYABLE_DRINK.matches(item))
			return;

		// buckets create their own ray, We use a fake wall to contain the active area
		Level itemUseWorld = world;
		if (item instanceof BucketItem || item instanceof SandPaperItem)
			itemUseWorld = new ItemUseWorld(world, face, pos);

		InteractionResultHolder<ItemStack> onItemRightClick = item.use(itemUseWorld, player, hand);
		ItemStack resultStack = onItemRightClick.getObject();
		if (resultStack != stack || resultStack.getCount() != stack.getCount() || resultStack.getUseDuration(player) > 0
			|| resultStack.getDamageValue() != stack.getDamageValue()) {
			player.setItemInHand(hand, onItemRightClick.getObject());
		}

		if (stack.getItem() instanceof SandPaperItem && stack.has(AllDataComponents.POLISHING)) {
			player.spawnedItemEffects = ItemStack.parseOptional(Create.getRegistryAccess(), 
					stack.get(AllDataComponents.POLISHING).getCompound("Polishing"));
			AllSoundEvents.SANDING_SHORT.playOnServer(world, pos, .25f, 1f);
		}

		if (!player.getUseItem()
			.isEmpty())
			player.setItemInHand(hand, stack.finishUsingItem(world, player));

		player.stopUsingItem();
	}

	public static boolean tryHarvestBlock(ServerPlayer player, ServerPlayerGameMode interactionManager, BlockPos pos) {
		// <> PlayerInteractionManager#tryHarvestBlock

		ServerLevel world = player.serverLevel();
		BlockState blockstate = world.getBlockState(pos);
		GameType gameType = interactionManager.getGameModeForPlayer();

		if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, world.getBlockState(pos), world.getBlockEntity(pos)))
			return false;

		BlockEntity blockEntity = world.getBlockEntity(pos);
//		if (player.getMainHandItem()
//			.onBlockStartBreak(pos, player))
//			return false;
		if (player.blockActionRestricted(world, pos, gameType))
			return false;

		ItemStack prevHeldItem = player.getMainHandItem();
		ItemStack heldItem = prevHeldItem.copy();

		boolean canHarvest = player.hasCorrectToolForDrops(blockstate) && player.mayBuild();
		prevHeldItem.mineBlock(world, blockstate, pos, player);
//		if (prevHeldItem.isEmpty() && !heldItem.isEmpty())
//			net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, heldItem, InteractionHand.MAIN_HAND);

		BlockPos posUp = pos.above();
		BlockState stateUp = world.getBlockState(posUp);
		if (blockstate.getBlock() instanceof DoublePlantBlock
			&& blockstate.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER
			&& stateUp.getBlock() == blockstate.getBlock()
			&& stateUp.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
			// hack to prevent DoublePlantBlock from dropping a duplicate item
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);
			world.setBlock(posUp, Blocks.AIR.defaultBlockState(), 35);
		} else {
			blockstate.getBlock().playerWillDestroy(world, pos, blockstate, player);
			if (!world.setBlock(pos, world.getFluidState(pos).getType().defaultFluidState().createLegacyBlock(), world.isClientSide ? 11 : 3))
				return true;
		}

		blockstate.getBlock()
			.destroy(world, pos, blockstate);
		if (!canHarvest)
			return true;

		Block.getDrops(blockstate, world, pos, blockEntity, player, prevHeldItem)
			.forEach(item -> player.getInventory().placeItemBackInInventory(item));
		blockstate.spawnAfterBreak(world, pos, prevHeldItem, true);
		return true;
	}

	public static InteractionResult safeOnUse(BlockState state, Level world, BlockPos pos, Player player,
		InteractionHand hand, BlockHitResult ray) {
		if (state.getBlock() instanceof BeehiveBlock)
			return safeOnBeehiveUse(state, world, pos, player, hand);
		return state.useItemOn(player.getItemInHand(hand), world, player, hand, ray).result();
	}

	protected static InteractionResult safeOnBeehiveUse(BlockState state, Level world, BlockPos pos, Player player,
		InteractionHand hand) {
		// <> BeehiveBlock#onUse

		BeehiveBlock block = (BeehiveBlock) state.getBlock();
		ItemStack prevHeldItem = player.getItemInHand(hand);
		int honeyLevel = state.getValue(BeehiveBlock.HONEY_LEVEL);
		boolean success = false;
		if (honeyLevel < 5)
			return InteractionResult.PASS;

		if (prevHeldItem.getItem() == Items.SHEARS) {
			world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BEEHIVE_SHEAR,
				SoundSource.NEUTRAL, 1.0F, 1.0F);
			// <> BeehiveBlock#dropHoneycomb
			player.getInventory().placeItemBackInInventory(new ItemStack(Items.HONEYCOMB, 3));
			prevHeldItem.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
			success = true;
		}

		if (prevHeldItem.getItem() == Items.GLASS_BOTTLE) {
			prevHeldItem.shrink(1);
			world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL,
				SoundSource.NEUTRAL, 1.0F, 1.0F);
			ItemStack honeyBottle = new ItemStack(Items.HONEY_BOTTLE);
			if (prevHeldItem.isEmpty())
				player.setItemInHand(hand, honeyBottle);
			else
				player.getInventory().placeItemBackInInventory(honeyBottle);
			success = true;
		}

		if (!success)
			return InteractionResult.PASS;

		block.resetHoneyLevel(world, state, pos);
		return InteractionResult.SUCCESS;
	}

}
