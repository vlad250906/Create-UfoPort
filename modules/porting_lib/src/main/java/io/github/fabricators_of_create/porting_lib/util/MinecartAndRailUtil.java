package io.github.fabricators_of_create.porting_lib.util;

import javax.annotation.Nullable;

import io.github.fabricators_of_create.porting_lib.extensions.extensions.BaseRailBlockExtensions;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.AbstractMinecartAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

public class MinecartAndRailUtil {

	// rails

	public static final TagKey<Block> ACTIVATOR_RAILS = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath("c", "rails/activator"));

	public static boolean isActivatorRail(Block rail) {
		return rail.builtInRegistryHolder().is(ACTIVATOR_RAILS);
	}

	public static RailShape getDirectionOfRail(BlockState state, BlockGetter world, BlockPos pos,
			@Nullable BaseRailBlock block) {
		return ((BaseRailBlockExtensions) state.getBlock()).getRailDirection(state, world, pos, block);
	}

	// carts

	public static double getMaximumSpeed(AbstractMinecart cart) {
		return ((AbstractMinecartAccessor) cart).port_lib$getMaxSpeed();
	}

	public static double getSlopeAdjustment() {
		return 0.0078125D;
	}
}
