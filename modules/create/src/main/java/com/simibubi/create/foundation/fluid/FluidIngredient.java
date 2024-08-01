package com.simibubi.create.foundation.fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.spongepowered.include.com.google.common.base.Objects;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.netty.handler.codec.DecoderException;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

public abstract class FluidIngredient implements Predicate<FluidStack> {

	public static final FluidIngredient EMPTY = new FluidStackIngredient();
	
	public static final Codec<FluidIngredient> CODEC = Codec.xor(FluidTagIngredient.CODEC, FluidStackIngredient.CODEC).xmap(
			either -> (FluidIngredient)((Object)either.map(tagValue -> tagValue, itemValue -> itemValue)), 
			value -> {
			        if (value instanceof FluidTagIngredient) {
			        	FluidTagIngredient tagValue = (FluidTagIngredient)value;
			            return Either.left(tagValue);
			        }
			        if (value instanceof FluidStackIngredient) {
			        	FluidStackIngredient itemValue = (FluidStackIngredient)value;
			            return Either.right(itemValue);
			        }
			        throw new UnsupportedOperationException("This is neither an FluidStackIngredient nor a FluidTagIngredient.");
			}
	);
	
	public static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, FluidIngredient>(){

		@Override
		public FluidIngredient decode(RegistryFriendlyByteBuf buf) {
			boolean isTagIngr = buf.readBoolean();
			long amount = buf.readLong();
			if(isTagIngr) {
				FluidTagIngredient fti = new FluidTagIngredient();
				int sz = buf.readInt();
				fti.matchingFluidStacks = new ArrayList<>();
				fti.amountRequired = amount;
				for(int i=0;i<sz;i++) {
					fti.matchingFluidStacks.add(FluidStack.STREAM_CODEC.decode(buf));
				}
				return fti;
			}else {
				FluidStackIngredient fsi = new FluidStackIngredient();
				fsi.fluid = BuiltInRegistries.FLUID.get(buf.readResourceLocation());
				fsi.components = DataComponentPatch.STREAM_CODEC.decode(buf);
				fsi.amountRequired = amount;
				return fsi;
			}
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, FluidIngredient ingr) {
			buf.writeBoolean(ingr instanceof FluidTagIngredient);
			buf.writeLong(ingr.amountRequired);
			if(ingr instanceof FluidTagIngredient fti) {
				// Tag has to be resolved on the server before sending
				List<FluidStack> matchingFluidStacks = fti.getMatchingFluidStacks();
				buf.writeInt(matchingFluidStacks.size());
				matchingFluidStacks.stream()
					.forEach(stack -> FluidStack.STREAM_CODEC.encode(buf, stack));
			}else if(ingr instanceof FluidStackIngredient fsi){
				buf.writeResourceLocation(BuiltInRegistries.FLUID.getKey(fsi.fluid));
				DataComponentPatch.STREAM_CODEC.encode(buf, fsi.components);
			}else {
				throw new IllegalArgumentException("FluidIngredient neither FluidTagIngredient or FluidStackIngredient: "+ingr);
			}
		}
		
	};

	public List<FluidStack> matchingFluidStacks;

	public static FluidIngredient fromTag(TagKey<Fluid> tag, long amount) {
		FluidTagIngredient ingredient = new FluidTagIngredient();
		ingredient.tag = tag;
		ingredient.amountRequired = amount;
		return ingredient;
	}

	public static FluidIngredient fromFluid(Fluid fluid, long amount) {
		FluidStackIngredient ingredient = new FluidStackIngredient();
		ingredient.fluid = fluid;
		ingredient.amountRequired = amount;
		ingredient.fixFlowing();
		return ingredient;
	}

	public static FluidIngredient fromFluidStack(FluidStack fluidStack) {
		FluidStackIngredient ingredient = new FluidStackIngredient();
		ingredient.fluid = fluidStack.getFluid();
		ingredient.amountRequired = fluidStack.getAmount();
		ingredient.fixFlowing();
		ingredient.components = ((PatchedDataComponentMap)fluidStack.getComponents()).asPatch();
		return ingredient;
	}

	protected long amountRequired;

	protected abstract boolean testInternal(FluidStack t);

	//protected abstract void readInternal(FriendlyByteBuf buffer);

	//protected abstract void writeInternal(FriendlyByteBuf buffer);

	//protected abstract void readInternal(JsonObject json);

	//protected abstract void writeInternal(JsonObject json);

	protected abstract List<FluidStack> determineMatchingFluidStacks();

	public long getRequiredAmount() {
		return amountRequired;
	}

	public List<FluidStack> getMatchingFluidStacks() {
		if (matchingFluidStacks != null)
			return matchingFluidStacks;
		return matchingFluidStacks = determineMatchingFluidStacks();
	}

	@Override
	public boolean test(FluidStack t) {
		if (t == null)
			throw new IllegalArgumentException("FluidStack cannot be null");
		return testInternal(t);
	}

//	public void write(FriendlyByteBuf buffer) {
//		buffer.writeBoolean(this instanceof FluidTagIngredient);
//		buffer.writeVarLong(amountRequired);
//		writeInternal(buffer);
//	}
//
//	public static FluidIngredient read(FriendlyByteBuf buffer) {
//		boolean isTagIngredient = buffer.readBoolean();
//		FluidIngredient ingredient = isTagIngredient ? new FluidTagIngredient() : new FluidStackIngredient();
//		ingredient.amountRequired = buffer.readVarLong();
//		ingredient.readInternal(buffer);
//		return ingredient;
//	}
//
//	public JsonObject serialize() {
//		JsonObject json = new JsonObject();
//		writeInternal(json);
//		json.addProperty("amount", amountRequired);
//		return json;
//	}

	public static boolean isFluidIngredient(@Nullable JsonElement je) {
		if (je == null || je.isJsonNull())
			return false;
		if (!je.isJsonObject())
			return false;
		JsonObject json = je.getAsJsonObject();
		if (json.has("fluidTag"))
			return true;
		else if (json.has("fluid"))
			return true;
		return false;
	}

//	public static FluidIngredient deserialize(@Nullable JsonElement je) {
//		if (!isFluidIngredient(je))
//			throw new JsonSyntaxException("Invalid fluid ingredient: " + Objects.toString(je));
//
//		JsonObject json = je.getAsJsonObject();
//		FluidIngredient ingredient = json.has("fluidTag") ? new FluidTagIngredient() : new FluidStackIngredient();
//		ingredient.readInternal(json);
//
//		if (!json.has("amount"))
//			throw new JsonSyntaxException("Fluid ingredient has to define an amount");
//		ingredient.amountRequired = GsonHelper.getAsInt(json, "amount");
//		return ingredient;
//	}

	public static class FluidStackIngredient extends FluidIngredient {

		protected Fluid fluid;
		protected DataComponentPatch components;
		
		public static final Codec<FluidStackIngredient> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(fsi -> fsi.fluid),
						Codec.LONG.fieldOf("amount").forGetter(fsi -> fsi.amountRequired),
						//CompoundTag.CODEC.optionalFieldOf("tag").forGetter(fsi -> Optional.ofNullable(fsi.tagToMatch))
						DataComponentPatch.CODEC.optionalFieldOf("components").forGetter(fsi -> Optional.ofNullable(fsi.components))
				).apply(instance, FluidStackIngredient::new)
		);

		public FluidStackIngredient(Fluid fluid, long amount, Optional<DataComponentPatch> tag) {
			this.fluid = fluid;
			this.components = tag.isEmpty() ? DataComponentPatch.EMPTY : tag.get();
			super.amountRequired = amount;
		}
		

		public FluidStackIngredient() {
			components = DataComponentPatch.EMPTY;
		}

		void fixFlowing() {
			if (fluid instanceof FlowingFluid)
				fluid = ((FlowingFluid) fluid).getSource();
		}

		@Override
		protected boolean testInternal(FluidStack t) {
			if (!t.getFluid()
				.isSame(fluid))
				return false;
			if(components.isEmpty()) return true;
			DataComponentMap dcm = t.getComponents().filter(dct -> components.get(dct).isPresent());
			boolean ok = true;
			for(Entry<DataComponentType<?>, Optional<?>> ent : components.entrySet()) {
				if(!Objects.equal(Optional.of(dcm.get(ent.getKey())), ent.getValue())) ok = false;
			}
			return ok;
//			if (tagToMatch.isEmpty())
//				return true;
//			CompoundTag tag = t.getOrCreateTag();
//			return tag.copy()
//				.merge(tagToMatch)
//				.equals(tag);
		}

//		@Override
//		protected void readInternal(FriendlyByteBuf buffer) {
//			fluid = BuiltInRegistries.FLUID.get(buffer.readResourceLocation());
//			tagToMatch = buffer.readNbt();
//		}
//
//		@Override
//		protected void writeInternal(FriendlyByteBuf buffer) {
//			buffer.writeResourceLocation(BuiltInRegistries.FLUID.getKey(fluid));
//			buffer.writeNbt(tagToMatch);
//		}
//
//		@Override
//		protected void readInternal(JsonObject json) {
//			FluidStack stack = FluidHelper.deserializeFluidStack(json);
//			fluid = stack.getFluid();
//			tagToMatch = stack.getOrCreateTag();
//		}
//
//		@Override
//		protected void writeInternal(JsonObject json) {
//			json.addProperty("fluid", RegisteredObjects.getKeyOrThrow(fluid)
//				.toString());
//			json.add("nbt", JsonParser.parseString(tagToMatch.toString()));
//		}

		@Override
		protected List<FluidStack> determineMatchingFluidStacks() {
			return ImmutableList.of(components.isEmpty() ? new FluidStack(fluid, amountRequired)
				: new FluidStack(FluidVariant.of(fluid, components), amountRequired));
		}

	}

	public static class FluidTagIngredient extends FluidIngredient {

		protected TagKey<Fluid> tag;
		
		public static final Codec<FluidTagIngredient> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						TagKey.codec(Registries.FLUID).fieldOf("fluidTag").forGetter(entry -> entry.tag),
						Codec.LONG.fieldOf("amount").forGetter(fsi -> fsi.amountRequired)
				).apply(instance, FluidTagIngredient::new)
		);
		
		public FluidTagIngredient(TagKey<Fluid> tk, long amount) {
			this.tag = tk;
			super.amountRequired = amount;
		}
		
		public FluidTagIngredient() {
			
		}

		@SuppressWarnings("deprecation")
		@Override
		protected boolean testInternal(FluidStack t) {
			if (tag == null) {
				for (FluidStack accepted : getMatchingFluidStacks())
					if (accepted.getFluid()
						.isSame(t.getFluid()))
						return true;
				return false;
			}
			return t.getFluid().is(tag);
		}

//		@Override
//		protected void readInternal(FriendlyByteBuf buffer) {
//			int size = buffer.readVarInt();
//			matchingFluidStacks = new ArrayList<>(size);
//			for (int i = 0; i < size; i++)
//				matchingFluidStacks.add(FluidStack.readFromPacket(buffer));
//		}
//
//		@Override
//		protected void writeInternal(FriendlyByteBuf buffer) {
//			// Tag has to be resolved on the server before sending
//			List<FluidStack> matchingFluidStacks = getMatchingFluidStacks();
//			buffer.writeVarInt(matchingFluidStacks.size());
//			matchingFluidStacks.stream()
//				.forEach(stack -> stack.writeToPacket(buffer));
//		}
//
//		@Override
//		protected void readInternal(JsonObject json) {
//			ResourceLocation name = ResourceLocation.fromNamespaceAndPath(GsonHelper.getAsString(json, "fluidTag"));
//			tag = TagKey.create(Registries.FLUID, name);
//		}
//
//		@Override
//		protected void writeInternal(JsonObject json) {
//			json.addProperty("fluidTag", tag.location()
//				.toString());
//		}

		@Override
		protected List<FluidStack> determineMatchingFluidStacks() {
			List<FluidStack> stacks = new ArrayList<>();
			for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
				Fluid f = holder.value();
				if (f instanceof FlowingFluid flowing) f = flowing.getSource();
				stacks.add(new FluidStack(f, amountRequired));
			}
			return stacks;
		}
		
		

	}

}
