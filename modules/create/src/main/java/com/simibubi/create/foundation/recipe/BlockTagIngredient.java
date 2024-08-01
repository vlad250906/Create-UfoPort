package com.simibubi.create.foundation.recipe;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.Create;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient.TagValue;
import net.minecraft.world.level.block.Block;

public class BlockTagIngredient implements CustomIngredient {
	protected final TagKey<Block> tag;

	protected BlockTagIngredient(TagKey<Block> tag) {
		this.tag = tag;
	}

	public static BlockTagIngredient create(TagKey<Block> tag) {
		return new BlockTagIngredient(tag);
	}

	public TagKey<Block> getTag() {
		return tag;
	}

	@Override
	public boolean requiresTesting() {
		return false;
	}

	@Override
	public List<ItemStack> getMatchingStacks() {
		ImmutableList.Builder<ItemStack> stacks = ImmutableList.builder();
		for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
			stacks.add(new ItemStack(block.value().asItem()));
		}
		return stacks.build();
	}

	@Override
	public boolean test(ItemStack stack) {
		return Block.byItem(stack.getItem()).defaultBlockState().is(tag);
	}

	@Override
	public CustomIngredientSerializer<?> getSerializer() {
		return Serializer.INSTANCE;
	}

	public static class Serializer implements CustomIngredientSerializer<BlockTagIngredient> {
		public static final ResourceLocation ID = Create.asResource("block_tag_ingredient");
		public static final Serializer INSTANCE = new Serializer();
		
		public static final MapCodec<BlockTagIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
				.group((TagKey.codec(Registries.BLOCK).fieldOf("tag")).forGetter(entry -> entry.tag))
				.apply(instance, BlockTagIngredient::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, BlockTagIngredient> STREAM_CODEC = StreamCodec.composite(
				ResourceLocation.STREAM_CODEC, bti -> bti.tag.location(),
				loc -> new BlockTagIngredient(TagKey.create(Registries.BLOCK, loc))
		);

		@Override
		public ResourceLocation getIdentifier() {
			return ID;
		}

		/*
		 * @Override public BlockTagIngredient read(JsonObject json) { ResourceLocation
		 * rl = ResourceLocation.fromNamespaceAndPath(GsonHelper.getAsString(json, "tag")); TagKey<Block>
		 * tag = TagKey.create(Registries.BLOCK, rl); return new
		 * BlockTagIngredient(tag); }
		 * 
		 * @Override public void write(JsonObject json, BlockTagIngredient ingredient) {
		 * json.addProperty("tag", ingredient.tag.location().toString()); }
		 */

//		@Override
//		public BlockTagIngredient read(FriendlyByteBuf buffer) {
//			ResourceLocation rl = buffer.readResourceLocation();
//			TagKey<Block> tag = TagKey.create(Registries.BLOCK, rl);
//			return new BlockTagIngredient(tag);
//		}
//
//		@Override
//		public void write(FriendlyByteBuf buf, BlockTagIngredient ingredient) {
//			buf.writeResourceLocation(ingredient.tag.location());
//		}

		@Override
		public MapCodec<BlockTagIngredient> getCodec(boolean allowEmpty) {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, BlockTagIngredient> getPacketCodec() {
			return STREAM_CODEC;
		}
	}
}
