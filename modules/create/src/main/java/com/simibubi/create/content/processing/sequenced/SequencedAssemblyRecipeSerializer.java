package com.simibubi.create.content.processing.sequenced;

import java.util.List;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SequencedAssemblyRecipeSerializer implements RecipeSerializer<SequencedAssemblyRecipe> {

	public SequencedAssemblyRecipeSerializer() {}

//	protected void writeToJson(JsonObject json, SequencedAssemblyRecipe recipe) {
//		JsonArray nestedRecipes = new JsonArray();
//		JsonArray results = new JsonArray();
//		json.add("ingredient", null); //recipe.getIngredient().toJson());
//		recipe.getSequence().forEach(i -> nestedRecipes.add(i.toJson()));
//		recipe.resultPool.forEach(p -> results.add(p.serialize()));
//		json.add("transitionalItem", recipe.transitionalItem.serialize());
//		json.add("sequence", nestedRecipes);
//		json.add("results", results);
//		json.addProperty("loops", recipe.loops);
//	}
//
//	protected SequencedAssemblyRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
//		SequencedAssemblyRecipe recipe = new SequencedAssemblyRecipe(recipeId, this);
//		recipe.ingredient = null; //Ingredient.fromJson(json.get("ingredient"));
//		recipe.transitionalItem = ProcessingOutput.deserialize(GsonHelper.getAsJsonObject(json, "transitionalItem"));
//		int i = 0;
//		for (JsonElement je : GsonHelper.getAsJsonArray(json, "sequence"))
//			recipe.getSequence().add(SequencedRecipe.fromJson(je.getAsJsonObject(), recipe, i++));
//		for (JsonElement je : GsonHelper.getAsJsonArray(json, "results"))
//			recipe.resultPool.add(ProcessingOutput.deserialize(je));
//		if (GsonHelper.isValidNode(json, "loops"))
//			recipe.loops = GsonHelper.getAsInt(json, "loops");
//		return recipe;
//	}

	protected void writeToBuffer(RegistryFriendlyByteBuf buffer, SequencedAssemblyRecipe recipe) {
		Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getIngredient());
		buffer.writeVarInt(recipe.getSequence().size());
		recipe.getSequence().forEach(sr -> sr.writeToBuffer(buffer));
		buffer.writeVarInt(recipe.resultPool.size());
		recipe.resultPool.forEach(sr -> ProcessingOutput.STREAM_CODEC.encode(buffer, sr));
		ProcessingOutput.STREAM_CODEC.encode(buffer, recipe.transitionalItem);
		buffer.writeInt(recipe.loops);
	}

	protected SequencedAssemblyRecipe readFromBuffer(ResourceLocation recipeId, RegistryFriendlyByteBuf buffer) {
		SequencedAssemblyRecipe recipe = new SequencedAssemblyRecipe(recipeId, this);
		recipe.ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
		int size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			recipe.getSequence().add(SequencedRecipe.readFromBuffer(buffer));
		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			recipe.resultPool.add(ProcessingOutput.STREAM_CODEC.decode(buffer));
		recipe.transitionalItem = ProcessingOutput.STREAM_CODEC.decode(buffer);
		recipe.loops = buffer.readInt();
		return recipe;
	}

//	public final void write(JsonObject json, SequencedAssemblyRecipe recipe) {
//		writeToJson(json, recipe);
//	}

//	@Override
//	public final void toNetwork(RegistryFriendlyByteBuf buffer, SequencedAssemblyRecipe recipe) {
//		writeToBuffer(buffer, recipe);
//	}
//
//	@Override
//	public final SequencedAssemblyRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
//		return readFromBuffer(ResourceLocation.fromNamespaceAndPath(""), buffer);
//	}
	
	private SequencedAssemblyRecipe buildRecipe(Ingredient ingr, ProcessingOutput trans, List<Recipe<?>> seq, List<ProcessingOutput> res, Optional<Integer> loops) {
		SequencedAssemblyRecipe recipe = new SequencedAssemblyRecipe(
				ResourceLocation.parse(""), this);
		recipe.ingredient = ingr;
		recipe.transitionalItem = trans;
		int i = 0;
		for (Recipe<?> rec : seq)
			recipe.getSequence().add(SequencedRecipe.fromParsed(rec, recipe, i++));
		for (ProcessingOutput pro : res)
			recipe.resultPool.add(pro);
		if (!loops.isEmpty())
			recipe.loops = loops.get();
		return recipe;
	}

	@Override
	public MapCodec<SequencedAssemblyRecipe> codec() {
		MapCodec<SequencedAssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Ingredient.CODEC.fieldOf("ingredient").forGetter(sar -> sar.ingredient),
						ProcessingOutput.CODEC.fieldOf("transitionalItem").forGetter(sar -> sar.transitionalItem),
						Recipe.CODEC.listOf().fieldOf("sequence").forGetter(sar -> sar.getRecipeSequence()),
						ProcessingOutput.CODEC.listOf().fieldOf("results").forGetter(sar -> sar.resultPool),
						Codec.INT.optionalFieldOf("loops").forGetter(sar -> Optional.ofNullable(sar.loops))
				).apply(instance, (ingr, trans, seq, res, loops) -> {
					return buildRecipe(ingr, trans, seq, res, loops);
				})
		);
		return CODEC;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, SequencedAssemblyRecipe> streamCodec() {
		return new StreamCodec<RegistryFriendlyByteBuf, SequencedAssemblyRecipe>(){

			@Override
			public SequencedAssemblyRecipe decode(RegistryFriendlyByteBuf buf) {
				return readFromBuffer(ResourceLocation.parse(""), buf);
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buf, SequencedAssemblyRecipe rec) {
				writeToBuffer(buf, rec);
			}
			
		};
	}

}
