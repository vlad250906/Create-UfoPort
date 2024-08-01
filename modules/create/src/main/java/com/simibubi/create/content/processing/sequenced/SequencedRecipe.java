package com.simibubi.create.content.processing.sequenced;

import java.util.Arrays;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonParseException;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SequencedRecipe<T extends ProcessingRecipe<?>> {
	
	private T wrapped;

	public SequencedRecipe(T wrapped) {
		this.wrapped = wrapped;
	}

	public IAssemblyRecipe getAsAssemblyRecipe() {
		return (IAssemblyRecipe) wrapped;
	}

	public ProcessingRecipe<?> getRecipe() {
		return wrapped;
	}

//	public JsonObject toJson() {
//		@SuppressWarnings("unchecked")
//		ProcessingRecipeSerializer<T> serializer = (ProcessingRecipeSerializer<T>) wrapped.getSerializer();
//		JsonObject json = new JsonObject();
//		json.addProperty("type", RegisteredObjects.getKeyOrThrow(serializer)
//			.toString());
//		serializer.write(json, wrapped);
//		return json;
//	}
//
//	public static SequencedRecipe<?> fromJson(JsonObject json, SequencedAssemblyRecipe parent, int index) {
//		ResourceLocation parentId = ResourceLocation.fromNamespaceAndPath(""); //parent.getId();
//		RecipeHolder<?> recipe = null; //RecipeManager.fromJson(
//			//ResourceLocation.fromNamespaceAndPath(parentId.getNamespace(), parentId.getPath() + "_step_" + index), json);
//		if (recipe.value() instanceof ProcessingRecipe<?> && recipe.value() instanceof IAssemblyRecipe) {
//			ProcessingRecipe<?> processingRecipe = (ProcessingRecipe<?>) recipe.value();
//			IAssemblyRecipe assemblyRecipe = (IAssemblyRecipe) recipe.value();
//			if (assemblyRecipe.supportsAssembly()) {
//				Ingredient transit = Ingredient.of(parent.getTransitionalItem());
//
//				processingRecipe.getIngredients()
//					.set(0, index == 0 ? Ingredient.fromValues(ImmutableList.of(transit, parent.getIngredient()).stream().flatMap(i -> Arrays.stream(i.values))) : transit);
//				SequencedRecipe<?> sequencedRecipe = new SequencedRecipe<>(processingRecipe);
//				return sequencedRecipe;
//			}
//		}
//		throw new JsonParseException("Not a supported recipe type");
//	}
	
	public static SequencedRecipe<?> fromParsed(Recipe<?> recipe, SequencedAssemblyRecipe parent, int index){
		if(recipe instanceof ProcessingRecipe<?> && recipe instanceof IAssemblyRecipe) {
			ProcessingRecipe<?> processingRecipe = (ProcessingRecipe<?>) recipe;
			IAssemblyRecipe assemblyRecipe = (IAssemblyRecipe) recipe;
			if (assemblyRecipe.supportsAssembly()) {
				Ingredient transit = Ingredient.of(parent.getTransitionalItem());

				processingRecipe.getIngredients()
					.set(0, index == 0 ? Ingredient.fromValues(ImmutableList.of(transit, parent.getIngredient()).stream().flatMap(i -> Arrays.stream(i.values))) : transit);
				SequencedRecipe<?> sequencedRecipe = new SequencedRecipe<>(processingRecipe);
				return sequencedRecipe;
			}
		}
		throw new JsonParseException("Not a supported recipe type");
	}

	public void writeToBuffer(RegistryFriendlyByteBuf buffer) {
		@SuppressWarnings("unchecked")
		ProcessingRecipeSerializer<T> serializer = (ProcessingRecipeSerializer<T>) wrapped.getSerializer();
		buffer.writeResourceLocation(RegisteredObjects.getKeyOrThrow(serializer));
		//buffer.writeResourceLocation(wrapped.getId());
		serializer.writeToBuffer(buffer, wrapped);
	}

	public static SequencedRecipe<?> readFromBuffer(RegistryFriendlyByteBuf buffer) {
		ResourceLocation resourcelocation = buffer.readResourceLocation();
		RecipeSerializer<?> serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(resourcelocation);
		if (!(serializer instanceof ProcessingRecipeSerializer))
			throw new JsonParseException("Not a supported recipe type");
		@SuppressWarnings("rawtypes")
		ProcessingRecipe recipe = (ProcessingRecipe) ((ProcessingRecipeSerializer)serializer).readFromBuffer(ResourceLocation.parse(""), buffer);
		return new SequencedRecipe<>(recipe);
	}

}
