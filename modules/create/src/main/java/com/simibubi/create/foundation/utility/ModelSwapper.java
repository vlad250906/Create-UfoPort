package com.simibubi.create.foundation.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.block.render.CustomBlockModels;
import com.simibubi.create.foundation.item.render.CustomItemModels;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItems;

import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.AfterBake;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModelSwapper implements AfterBake {

	protected CustomBlockModels customBlockModels = new CustomBlockModels();
	protected CustomItemModels customItemModels = new CustomItemModels();

	private Map<ResourceLocation, NonNullFunction<BakedModel, ? extends BakedModel>> swaps = null;

	public CustomBlockModels getCustomBlockModels() {
		return customBlockModels;
	}

	public CustomItemModels getCustomItemModels() {
		return customItemModels;
	}

	public void registerListeners() {
		ModelLoadingPlugin.register(ctx -> ctx.modifyModelAfterBake().register(this));
	}

	@Override
	public BakedModel modifyModelAfterBake(BakedModel model, Context context) {
		if (swaps == null)
			collectSwaps();
		ResourceLocation id = context.resourceId() == null ? context.topLevelId().id() : context.resourceId();
		NonNullFunction<BakedModel, ? extends BakedModel> swap = swaps.get(id);
		//LogUtils.getLogger().info("Swap: "+id+" -> "+swap);
		return swap != null ? swap.apply(model) : model;
	}

	private void collectSwaps() {
		this.swaps = new HashMap<>();

		customBlockModels.forEach((block, swapper) -> getAllBlockStateModelLocations(block).forEach(id -> swaps.put(id.id(), swapper)));
		customItemModels.forEach((item, swapper) -> swaps.put(getItemModelLocation(item).id(), swapper));
		CustomRenderedItems.forEach(item -> swaps.put(getItemModelLocation(item).id(), CustomRenderedItemModel::new));
	}

	public static List<ModelResourceLocation> getAllBlockStateModelLocations(Block block) {
		List<ModelResourceLocation> models = new ArrayList<>();
		ResourceLocation blockRl = RegisteredObjects.getKeyOrThrow(block);
		block.getStateDefinition()
			.getPossibleStates()
			.forEach(state -> {
				models.add(BlockModelShaper.stateToModelLocation(blockRl, state));
			});
		return models;
	}

	public static ModelResourceLocation getItemModelLocation(Item item) {
		return new ModelResourceLocation(RegisteredObjects.getKeyOrThrow(item), "inventory");
	}

}
