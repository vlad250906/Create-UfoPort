package com.simibubi.create.content.processing.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeFactory;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ProcessingRecipeSerializer<T extends ProcessingRecipe<?>> implements RecipeSerializer<T> {
	
	private final ProcessingRecipeFactory<T> factory;
	private MapCodec<T> CODEC = null;
	private StreamCodec<RegistryFriendlyByteBuf, T> STREAM_CODEC = null;

	public ProcessingRecipeSerializer(ProcessingRecipeFactory<T> factory, ProcessingCodecBuilder builder) {
		this.factory = factory;
		builder.setGetterList(obj -> this.toJson((T)obj));
		this.CODEC = builder.build(((List<Object> args) -> {
			return buildFromJson((List<Object>)args.get(0), (List<Object>)args.get(1), (Optional<Integer>)args.get(2), 
					(Optional<String>)args.get(3), args.subList(4, 16));
		}));
		STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, T>(){

			@Override
			public T decode(RegistryFriendlyByteBuf buf) {
				return readFromBuffer(ResourceLocation.parse(""), buf);
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buf, T val) {
				writeToBuffer(buf, val);
			}
			
		};
	}
	
	@Override
	public MapCodec<T> codec() {
		return CODEC;
	}
	
	@Override
	public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
		return STREAM_CODEC;
	}

	public ProcessingRecipeFactory<T> getFactory() {
		return factory;
	}
	
	public void writeToBuffer(RegistryFriendlyByteBuf buffer, T recipe) {
		NonNullList<Ingredient> ingredients = recipe.ingredients;
		NonNullList<FluidIngredient> fluidIngredients = recipe.fluidIngredients;
		NonNullList<ProcessingOutput> outputs = recipe.results;
		NonNullList<FluidStack> fluidOutputs = recipe.fluidResults;

		buffer.writeVarInt(ingredients.size());
		ingredients.forEach(i -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, i));
		buffer.writeVarInt(fluidIngredients.size());
		fluidIngredients.forEach(i -> FluidIngredient.STREAM_CODEC.encode(buffer, i));

		buffer.writeVarInt(outputs.size());
		outputs.forEach(o -> ProcessingOutput.STREAM_CODEC.encode(buffer, o));
		buffer.writeVarInt(fluidOutputs.size());
		fluidOutputs.forEach(o -> FluidStack.STREAM_CODEC.encode(buffer, o));

		buffer.writeVarInt(recipe.getProcessingDuration());
		buffer.writeVarInt(recipe.getRequiredHeat()
			.ordinal());

		recipe.writeAdditional(buffer);
	}

	public T readFromBuffer(ResourceLocation recipeId, RegistryFriendlyByteBuf buffer) {
		NonNullList<Ingredient> ingredients = NonNullList.create();
		NonNullList<FluidIngredient> fluidIngredients = NonNullList.create();
		NonNullList<ProcessingOutput> results = NonNullList.create();
		NonNullList<FluidStack> fluidResults = NonNullList.create();

		int size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));

		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			fluidIngredients.add(FluidIngredient.STREAM_CODEC.decode(buffer));

		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			results.add(ProcessingOutput.STREAM_CODEC.decode(buffer));

		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			fluidResults.add(FluidStack.STREAM_CODEC.decode(buffer));

		T recipe = new ProcessingRecipeBuilder<>(factory, recipeId).withItemIngredients(ingredients)
			.withItemOutputs(results)
			.withFluidIngredients(fluidIngredients)
			.withFluidOutputs(fluidResults)
			.duration(buffer.readVarInt())
			.requiresHeat(HeatCondition.values()[buffer.readVarInt()])
			.build();
		recipe.readAdditional(buffer);
		return recipe;
	}
	
	public T buildFromJson(List<Object> ingr, List<Object> res, Optional<Integer> time, Optional<String> heat, List<Object> extra) {
		ProcessingRecipeBuilder<T> builder = new ProcessingRecipeBuilder<>(factory, 
				ResourceLocation.parse(""));
		NonNullList<Ingredient> ingredients = NonNullList.create();
		NonNullList<FluidIngredient> fluidIngredients = NonNullList.create();
		NonNullList<ProcessingOutput> results = NonNullList.create();
		NonNullList<FluidStack> fluidResults = NonNullList.create();
		for(Object ob : ingr) {
			if (ob instanceof FluidIngredient) {
				fluidIngredients.add((FluidIngredient)ob);
			}else {
				ingredients.add((Ingredient)ob);
			}
		}
		for(Object ob : res) {
			if (ob instanceof FluidStack) {
				fluidResults.add((FluidStack)ob);
			}else {
				results.add((ProcessingOutput)ob);
			}
		}
		builder.withItemIngredients(ingredients)
		.withItemOutputs(results)
		.withFluidIngredients(fluidIngredients)
		.withFluidOutputs(fluidResults);

		if (!time.isEmpty())
			builder.duration(time.get());
		if (!heat.isEmpty())
			builder.requiresHeat(HeatCondition.deserialize(heat.get()));
		
		T recipe = builder.build();
//		if(recipe instanceof ItemApplicationRecipe) {
//			((ItemApplicationRecipe)recipe).keepHeldItem = held.isEmpty() ? false : held.get();
//		}
		recipe.readAdditional(extra);
		return recipe;
	}
	
	public List<Object> toJson(T recipe){
		List<Object> result = new ArrayList<Object>();
		List<Object> input = new ArrayList<Object>();
		List<Object> output = new ArrayList<Object>();
		
		input.addAll(recipe.fluidIngredients);
		input.addAll(recipe.ingredients);
		output.addAll(recipe.fluidResults);
		output.addAll(recipe.results);
		int time = recipe.processingDuration;
		HeatCondition heat = recipe.requiredHeat;
		
		result.add(input);
		result.add(output);
		result.add(time == 0 ? Optional.empty() : Optional.of(time));
		result.add(heat == HeatCondition.NONE ? Optional.empty() : Optional.of(heat.serialize()));
		recipe.writeAdditional(result);
		
		return result;
	}
	
	public static final Codec<Object> CODEC_INGREDIENT = Codec.xor(Ingredient.CODEC, FluidIngredient.CODEC).xmap(
			either -> ((Object)either.map(itemValue -> itemValue, fluidValue -> fluidValue)), 
			value -> {
			        if (value instanceof Ingredient) {
			            return Either.left((Ingredient)value);
			        }
			        if (value instanceof FluidIngredient) {
			            return Either.right((FluidIngredient)value);
			        }
			        throw new UnsupportedOperationException("This is neither an FluidStackIngredient nor a FluidTagIngredient.");
			}
	);	
	
	public static final Codec<Object> CODEC_RESULT = Codec.xor(FluidStack.CODEC, ProcessingOutput.CODEC).xmap(
			either -> ((Object)either.map(itemValue -> itemValue, fluidValue -> fluidValue)), 
			value -> {
			        if (value instanceof FluidStack) {
			            return Either.left((FluidStack)value);
			        }
			        if (value instanceof ProcessingOutput) {
			            return Either.right((ProcessingOutput)value);
			        }
			        throw new UnsupportedOperationException("This is neither an FluidStackIngredient nor a FluidTagIngredient.");
			}
	);

	


}
