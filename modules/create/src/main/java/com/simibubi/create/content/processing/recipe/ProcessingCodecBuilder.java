package com.simibubi.create.content.processing.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.world.item.crafting.Ingredient;

public class ProcessingCodecBuilder {
	
	private List<RecordCodecBuilder> fragments = new ArrayList<RecordCodecBuilder>();
	private Function<ProcessingRecipe, List<Object>> supplier;
	
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

	public List<Object> getGetterList(Object obj) {
		return supplier.apply((ProcessingRecipe)obj);
	}

	public void setGetterList(Function<ProcessingRecipe, List<Object>> supplier) {
		this.supplier = supplier;
	}

	public ProcessingCodecBuilder addField(RecordCodecBuilder fr) {
		fragments.add(fr);
		return this;
	}
	
	public <T extends ProcessingRecipe<?>> MapCodec<T> build(Function<List<Object>, T> factory){
		MapCodec<T> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						fragments.size() > 0 ? fragments.get(0) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_0").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 1 ? fragments.get(1) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_1").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 2 ? fragments.get(2) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_2").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 3 ? fragments.get(3) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_3").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 4 ? fragments.get(4) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_4").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 5 ? fragments.get(5) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_5").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 6 ? fragments.get(6) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_6").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 7 ? fragments.get(7) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_7").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 8 ? fragments.get(8) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_8").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 9 ? fragments.get(9) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_9").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 10 ? fragments.get(10) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_10").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 11 ? fragments.get(11) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_11").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 12 ? fragments.get(12) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_12").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 13 ? fragments.get(13) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_13").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 14 ? fragments.get(14) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_14").forGetter((O) -> Optional.ofNullable(null)),
						fragments.size() > 15 ? fragments.get(15) : 
							Codec.STRING.optionalFieldOf("ur483583vnv2vbnfv98nmui4_15").forGetter((O) -> Optional.ofNullable(null))
				).apply(instance, (a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) -> {
						Object[] arr = new Object[4];
						List<Object> args = new ArrayList<Object>();
						args.add(a0);
						args.add(a1);
						args.add(a2);
						args.add(a3);
						args.add(a4);
						args.add(a5);
						args.add(a6);
						args.add(a7);
						args.add(a8);
						args.add(a9);
						args.add(a10);
						args.add(a11);
						args.add(a12);
						args.add(a13);
						args.add(a14);
						args.add(a15);
						return factory.apply(args);
				})
		);
		return CODEC;
	}
	
	public static ProcessingCodecBuilder getDefaultBuilder() {
		final ProcessingCodecBuilder builder = new ProcessingCodecBuilder();
		builder.addField(CODEC_INGREDIENT.listOf().fieldOf("ingredients")
				.forGetter(obj -> (List)builder.getGetterList(obj).get(0)));
		builder.addField(CODEC_RESULT.listOf().fieldOf("results")
				.forGetter(obj -> (List)builder.getGetterList(obj).get(1)));
		builder.addField(Codec.INT.optionalFieldOf("processingTime")
				.forGetter(obj -> (Optional)builder.getGetterList(obj).get(2)));
		builder.addField(Codec.STRING.optionalFieldOf("heatRequirement")
				.forGetter(obj -> (Optional)builder.getGetterList(obj).get(3)));
		return builder;
	}
	
	
}
