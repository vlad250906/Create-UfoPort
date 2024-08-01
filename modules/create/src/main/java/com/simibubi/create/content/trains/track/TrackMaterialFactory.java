package com.simibubi.create.content.trains.track;

import java.util.function.Supplier;
import java.util.stream.Stream;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.TagValueAccessor;

import org.jetbrains.annotations.Nullable;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllTags;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class TrackMaterialFactory {
	private final ResourceLocation id;
	private String langName;
	private NonNullSupplier<NonNullSupplier<? extends TrackBlock>> trackBlock;
	private Ingredient sleeperIngredient = Ingredient.EMPTY;
	private Ingredient railsIngredient = Ingredient.fromValues(Stream.of(
			TagValueAccessor.createTagValue(AllTags.forgeItemTag("iron_nuggets")),
			TagValueAccessor.createTagValue(AllTags.forgeItemTag("zinc_nuggets"))
	));
	private ResourceLocation particle;
	private TrackMaterial.TrackType trackType = TrackMaterial.TrackType.STANDARD;

	@Nullable
	private TrackMaterial.TrackType.TrackBlockFactory customFactory = null;

	@Environment(EnvType.CLIENT)
	private TrackMaterial.TrackModelHolder modelHolder;
	@Environment(EnvType.CLIENT)
	private PartialModel tieModel;
	@Environment(EnvType.CLIENT)
	private PartialModel leftSegmentModel;
	@Environment(EnvType.CLIENT)
	private PartialModel rightSegmentModel;

	public TrackMaterialFactory(ResourceLocation id) {
		this.id = id;
	}

	public static TrackMaterialFactory make(ResourceLocation id) {  // Convenience function for static import
		return new TrackMaterialFactory(id);
	}

	public TrackMaterialFactory lang(String langName) {
		this.langName = langName;
		return this;
	}

	public TrackMaterialFactory block(NonNullSupplier<NonNullSupplier<? extends TrackBlock>> trackBlock) {
		this.trackBlock = trackBlock;
		return this;
	}

	public TrackMaterialFactory defaultModels() { // was setBuiltin
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> this.modelHolder = TrackMaterial.TrackModelHolder.DEFAULT);
		return this;
	}

	public TrackMaterialFactory sleeper(Ingredient sleeperIngredient) {
		this.sleeperIngredient = sleeperIngredient;
		return this;
	}

	public TrackMaterialFactory sleeper(ItemLike... items) {
		this.sleeperIngredient = Ingredient.of(items);
		return this;
	}

	public TrackMaterialFactory rails(Ingredient railsIngredient) {
		this.railsIngredient = railsIngredient;
		return this;
	}

	public TrackMaterialFactory rails(ItemLike... items) {
		this.railsIngredient = Ingredient.of(items);
		return this;
	}

	public TrackMaterialFactory noRecipeGen() {
		this.railsIngredient = Ingredient.EMPTY;
		this.sleeperIngredient = Ingredient.EMPTY;
		return this;
	}

	public TrackMaterialFactory particle(ResourceLocation particle) {
		this.particle = particle;
		return this;
	}

	public TrackMaterialFactory trackType(TrackMaterial.TrackType trackType) {
		this.trackType = trackType;
		return this;
	}

	public TrackMaterialFactory standardModels() { // was defaultModels
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			String namespace = id.getNamespace();
			String prefix = "block/track/" + id.getPath() + "/";
			tieModel = new PartialModel(ResourceLocation.fromNamespaceAndPath(namespace, prefix + "tie"));
			leftSegmentModel = new PartialModel(ResourceLocation.fromNamespaceAndPath(namespace, prefix + "segment_left"));
			rightSegmentModel = new PartialModel(ResourceLocation.fromNamespaceAndPath(namespace, prefix + "segment_right"));
		});
		return this;
	}

	public TrackMaterialFactory customModels(Supplier<Supplier<PartialModel>> tieModel, Supplier<Supplier<PartialModel>> leftSegmentModel, Supplier<Supplier<PartialModel>> rightSegmentModel) {
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			this.tieModel = tieModel.get().get();
			this.leftSegmentModel = leftSegmentModel.get().get();
			this.rightSegmentModel = rightSegmentModel.get().get();
		});
		return this;
	}

	public TrackMaterialFactory customBlockFactory(TrackMaterial.TrackType.TrackBlockFactory factory) {
		this.customFactory = factory;
		return this;
	}

	public TrackMaterial build() {
		assert trackBlock != null;
		assert langName != null;
		assert particle != null;
		assert trackType != null;
		assert sleeperIngredient != null;
		assert railsIngredient != null;
		assert id != null;
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
			assert modelHolder != null;
			if (tieModel != null || leftSegmentModel != null || rightSegmentModel != null) {
				assert tieModel != null && leftSegmentModel != null && rightSegmentModel != null;
				modelHolder = new TrackMaterial.TrackModelHolder(tieModel, leftSegmentModel, rightSegmentModel);
			}
		});
		return new TrackMaterial(id, langName, trackBlock, particle, sleeperIngredient, railsIngredient, trackType, () -> () -> modelHolder, customFactory);
	}
}
