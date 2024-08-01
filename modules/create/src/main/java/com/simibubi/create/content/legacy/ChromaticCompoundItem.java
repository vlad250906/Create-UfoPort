package com.simibubi.create.content.legacy;

import java.util.Random;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.BeaconBlockEntityAccessor;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CRecipes;

import io.github.fabricators_of_create.porting_lib.block.LightEmissiveBlock;
import io.github.fabricators_of_create.porting_lib.item.CustomMaxCountItem;
import io.github.fabricators_of_create.porting_lib.item.EntityTickListenerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class ChromaticCompoundItem extends Item implements CustomMaxCountItem, EntityTickListenerItem {

	public ChromaticCompoundItem(Properties properties) {
		super(properties);
	}

	public int getLight(ItemStack stack) {
		return stack.getOrDefault(AllDataComponents.COLLECTING_LIGHT, 0);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return getLight(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0F * getLight(stack) / AllConfigs.server().recipes.lightSourceCountForRefinedRadiance.get());
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return Color.mixColors(0x413c69, 0xFFFFFF,
			getLight(stack) / (float) AllConfigs.server().recipes.lightSourceCountForRefinedRadiance.get());
	}

	@Override
	public int getItemStackLimit(ItemStack stack) {
		return isBarVisible(stack) ? 1 : 16;
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		Level world = entity.level();
		Vec3 positionVec = entity.position();
		CRecipes config = AllConfigs.server().recipes;

		if (world.isClientSide) {
			int light = stack.getOrDefault(AllDataComponents.COLLECTING_LIGHT, 0);
			if (world.random.nextInt(config.lightSourceCountForRefinedRadiance.get() + 20) < light) {
				Vec3 start = VecHelper.offsetRandomly(positionVec, world.random, 3);
				Vec3 motion = positionVec.subtract(start)
					.normalize()
					.scale(.2f);
				world.addParticle(ParticleTypes.END_ROD, start.x, start.y, start.z, motion.x, motion.y, motion.z);
			}
			return false;
		}

		double y = entity.getY();
		double yMotion = entity.getDeltaMovement().y;
		int minHeight = world.getMinBuildHeight();
		CompoundTag data = entity.getCustomData();

		// Convert to Shadow steel if in void
		if (y < minHeight && y - yMotion < -10 + minHeight && config.enableShadowSteelRecipe.get()) {
			ItemStack newStack = AllItems.SHADOW_STEEL.asStack();
			newStack.setCount(stack.getCount());
			data.putBoolean("JustCreated", true);
			entity.setItem(newStack);
		}

		if (!config.enableRefinedRadianceRecipe.get())
			return false;

		// Convert to Refined Radiance if eaten enough light sources
		if (stack.getOrDefault(AllDataComponents.COLLECTING_LIGHT, 0) >= config.lightSourceCountForRefinedRadiance.get()) {
			ItemStack newStack = AllItems.REFINED_RADIANCE.asStack();
			ItemEntity newEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), newStack);
			newEntity.setDeltaMovement(entity.getDeltaMovement());
			newEntity.getCustomData()
				.putBoolean("JustCreated", true);
			if(stack.has(AllDataComponents.COLLECTING_LIGHT))
				stack.remove(AllDataComponents.COLLECTING_LIGHT);
			world.addFreshEntity(newEntity);

			stack.split(1);
			entity.setItem(stack);
			if (stack.isEmpty())
				entity.discard();
			return false;
		}

		// Is inside beacon beam?
		boolean isOverBeacon = false;
		int entityX = Mth.floor(entity.getX());
		int entityZ = Mth.floor(entity.getZ());
		int localWorldHeight = world.getHeight(Heightmap.Types.WORLD_SURFACE, entityX, entityZ);

		BlockPos.MutableBlockPos testPos =
			new BlockPos.MutableBlockPos(entityX, Math.min(Mth.floor(entity.getY()), localWorldHeight), entityZ);

		while (testPos.getY() > 0) {
			testPos.move(Direction.DOWN);
			BlockState state = world.getBlockState(testPos);
			if (state.getLightBlock(world, testPos) >= 15 && state.getBlock() != Blocks.BEDROCK)
				break;
			if (state.getBlock() == Blocks.BEACON) {
				BlockEntity be = world.getBlockEntity(testPos);

				if (!(be instanceof BeaconBlockEntity))
					break;

				BeaconBlockEntity bte = (BeaconBlockEntity) be;

				if (!((BeaconBlockEntityAccessor) bte).port_lib$getBeamSections().isEmpty())
					isOverBeacon = true;

				break;
			}
		}

		if (isOverBeacon) {
			ItemStack newStack = AllItems.REFINED_RADIANCE.asStack();
			newStack.setCount(stack.getCount());
			data.putBoolean("JustCreated", true);
			entity.setItem(newStack);
			return false;
		}

		// Find a light source and eat it.
		RandomSource r = world.random;
		int range = 3;
		float rate = 1 / 2f;
		if (r.nextFloat() > rate)
			return false;

		BlockPos randomOffset = BlockPos.containing(VecHelper.offsetRandomly(positionVec, r, range));
		BlockState state = world.getBlockState(randomOffset);

		TransportedItemStackHandlerBehaviour behaviour =
			BlockEntityBehaviour.get(world, randomOffset, TransportedItemStackHandlerBehaviour.TYPE);

		// Find a placed light source
		if (behaviour == null) {
			if (checkLight(stack, entity, world, positionVec, randomOffset, state))
				world.destroyBlock(randomOffset, false);
			return false;
		}

		// Find a light source from a depot/belt (chunk rebuild safe)
		MutableBoolean success = new MutableBoolean(false);
		behaviour.handleProcessingOnAllItems(ts -> {

			ItemStack heldStack = ts.stack;
			if (!(heldStack.getItem() instanceof BlockItem))
				return TransportedResult.doNothing();

			BlockItem blockItem = (BlockItem) heldStack.getItem();
			if (blockItem.getBlock() == null)
				return TransportedResult.doNothing();

			BlockState stateToCheck = blockItem.getBlock()
				.defaultBlockState();

			if (!success.getValue()
				&& checkLight(stack, entity, world, positionVec, randomOffset, stateToCheck)) {
				success.setTrue();
				if (ts.stack.getCount() == 1)
					return TransportedResult.removeItem();
				TransportedItemStack left = ts.copy();
				left.stack.shrink(1);
				return TransportedResult.convertTo(left);
			}

			return TransportedResult.doNothing();

		});
		return false;
	}

	public boolean checkLight(ItemStack stack, ItemEntity entity, Level world, Vec3 positionVec,
		BlockPos randomOffset, BlockState state) {
		if(state.getBlock() instanceof LightEmissiveBlock lightEmissiveBlock && lightEmissiveBlock.getLightEmission(state, world, randomOffset) == 0)
			return false;
		else if (state.getLightEmission() == 0)
			return false;
		if (state.getDestroySpeed(world, randomOffset) == -1)
			return false;
		if (state.getBlock() == Blocks.BEACON)
			return false;

		ClipContext context = new ClipContext(positionVec.add(new Vec3(0, 0.5, 0)), VecHelper.getCenterOf(randomOffset),
			Block.COLLIDER, Fluid.NONE, entity);
		if (!randomOffset.equals(world.clip(context)
			.getBlockPos()))
			return false;

		ItemStack newStack = stack.split(1);
		newStack.set(AllDataComponents.COLLECTING_LIGHT, newStack.getOrDefault(AllDataComponents.COLLECTING_LIGHT, 0) + 1);
//		newStack.getOrCreateTag()
//			.putInt("CollectingLight", itemData.getInt("CollectingLight") + 1);
		ItemEntity newEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), newStack);
		newEntity.setDeltaMovement(entity.getDeltaMovement());
		newEntity.setDefaultPickUpDelay();
		world.addFreshEntity(newEntity);
//		entity.lifespan = 6000;
		if (stack.isEmpty())
			entity.discard();
		return true;
	}

}
