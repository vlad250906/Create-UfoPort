package com.simibubi.create;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.equipment.toolbox.ToolboxDyeingRecipe;
import com.simibubi.create.content.fluids.transfer.EmptyingRecipe;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import com.simibubi.create.content.kinetics.fan.processing.HauntingRecipe;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.mixer.CompactingRecipe;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeFactory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipeSerializer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.util.ShapedRecipeUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

public enum AllRecipeTypes implements IRecipeTypeInfo {

	CRUSHING(CrushingRecipe::new, CrushingRecipe.defaultCodec()), CUTTING(CuttingRecipe::new, CuttingRecipe.defaultCodec()), 
	MILLING(MillingRecipe::new, MillingRecipe.defaultCodec()), BASIN(BasinRecipe::new, BasinRecipe.defaultCodec()),
	MIXING(MixingRecipe::new, MixingRecipe.defaultCodec()), COMPACTING(CompactingRecipe::new, CompactingRecipe.defaultCodec()), 
	PRESSING(PressingRecipe::new, PressingRecipe.defaultCodec()), SANDPAPER_POLISHING(SandPaperPolishingRecipe::new, SandPaperPolishingRecipe.defaultCodec()),
	SPLASHING(SplashingRecipe::new, SplashingRecipe.defaultCodec()), HAUNTING(HauntingRecipe::new, HauntingRecipe.defaultCodec()),
	DEPLOYING(DeployerApplicationRecipe::new, DeployerApplicationRecipe.getCodecIA()), FILLING(FillingRecipe::new, FillingRecipe.defaultCodec()), 
	EMPTYING(EmptyingRecipe::new, EmptyingRecipe.defaultCodec()), ITEM_APPLICATION(ManualApplicationRecipe::new, ManualApplicationRecipe.getCodecIA()),

	MECHANICAL_CRAFTING(MechanicalCraftingRecipe.Serializer::new),
	SEQUENCED_ASSEMBLY(SequencedAssemblyRecipeSerializer::new),

	TOOLBOX_DYEING(() -> new SimpleCraftingRecipeSerializer<>(ToolboxDyeingRecipe::new), ToolboxDyeingRecipe::new, () -> RecipeType.CRAFTING,
			false);

	private final ResourceLocation id;
	private final RecipeSerializer<?> serializerObject;
	@Nullable
	private final RecipeType<?> typeObject;
	private final Supplier<RecipeType<?>> type;
	private final Function<CraftingBookCategory, Recipe<?>> simpleFactory;

	AllRecipeTypes(Supplier<RecipeSerializer<?>> serializerSupplier, Function<CraftingBookCategory, Recipe<?>> simpleFactory, Supplier<RecipeType<?>> typeSupplier,
			boolean registerType) {
		String name = Lang.asId(name());
		id = Create.asResource(name);
		this.simpleFactory = simpleFactory;
		serializerObject = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, serializerSupplier.get());
		if (registerType) {
			typeObject = typeSupplier.get();
			Registry.register(BuiltInRegistries.RECIPE_TYPE, id, typeObject);
			type = typeSupplier;
		} else {
			typeObject = null;
			type = typeSupplier;
		}
	}

	AllRecipeTypes(Supplier<RecipeSerializer<?>> serializerSupplier) {
		String name = Lang.asId(name());
		simpleFactory = null;
		id = Create.asResource(name);
		serializerObject = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, serializerSupplier.get());
		typeObject = simpleType(id);
		Registry.register(BuiltInRegistries.RECIPE_TYPE, id, typeObject);
		type = () -> typeObject;
	}

	AllRecipeTypes(ProcessingRecipeFactory<?> processingFactory, ProcessingCodecBuilder codecBuilder) {
		this(() -> new ProcessingRecipeSerializer<>(processingFactory, codecBuilder));
	}

	public static <T extends Recipe<?>> RecipeType<T> simpleType(ResourceLocation id) {
		String stringId = id.toString();
		return new RecipeType<T>() {
			@Override
			public String toString() {
				return stringId;
			}
		};
	}

	public static void register() {
		//System.out.println("All recipe types register!!!");
		ShapedRecipeUtil.setCraftingSize(9, 9);
		// fabric: just load the class
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	public Function<CraftingBookCategory, Recipe<?>> getSimpleFactory() {
		return simpleFactory;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends RecipeSerializer<?>> T getSerializer() {
		return (T) serializerObject;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends RecipeType<?>> T getType() {
		return (T) type.get();
	}

	public <C extends RecipeInput, T extends Recipe<C>> Optional<T> find(C inv, Level world) {
		Optional<RecipeHolder<Recipe<C>>> cc = world.getRecipeManager().getRecipeFor(getType(), inv, world);
		if (cc.isEmpty()) {
			return (Optional<T>) Optional.ofNullable((T) null);
		}
		return (Optional<T>) Optional.ofNullable(cc.get().value());
	}

	public static boolean shouldIgnoreInAutomation(Recipe<?> recipe) {
		RecipeSerializer<?> serializer = recipe.getSerializer();
		if (serializer != null && AllTags.AllRecipeSerializerTags.AUTOMATION_IGNORE.matches(serializer))
			return true;
		return false;
		// return recipe.getId()
		// .getPath()
		// .endsWith("_manual_only");
	}
}
