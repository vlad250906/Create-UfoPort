package com.simibubi.create.content.kinetics.crafter;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllRecipeTypes;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapedRecipePattern.Data;
import net.minecraft.world.level.Level;

public class MechanicalCraftingRecipe extends ShapedRecipe {

	private boolean acceptMirrored;

	public MechanicalCraftingRecipe(ResourceLocation idIn, String groupIn, int recipeWidthIn, int recipeHeightIn,
		NonNullList<Ingredient> recipeItemsIn, Optional<Data> data, ItemStack recipeOutputIn, boolean acceptMirrored) {
		super(groupIn, CraftingBookCategory.MISC, new ShapedRecipePattern(recipeWidthIn, recipeHeightIn, recipeItemsIn, data), recipeOutputIn);
		//super(groupIn, CraftingBookCategory.MISC, null, recipeOutputIn);
		this.acceptMirrored = acceptMirrored;
	}

	public static MechanicalCraftingRecipe fromShaped(ShapedRecipe recipe, boolean acceptMirrored) {
		return new MechanicalCraftingRecipe(ResourceLocation.parse(""), recipe.getGroup(), recipe.getWidth(), recipe.getHeight(),
			recipe.getIngredients(), recipe.pattern.data, recipe.getResultItem(null), acceptMirrored);
	}

	@Override
	public boolean matches(CraftingInput inv, Level worldIn) {
		//if (!(inv instanceof MechanicalCraftingInventory))
			//return false;
		if (acceptsMirrored())
			return super.matches(inv, worldIn);

		// From ShapedRecipe except the symmetry
		for (int i = 0; i <= inv.width() - this.getWidth(); ++i)
			for (int j = 0; j <= inv.height() - this.getHeight(); ++j)
				if (this.matchesSpecific(inv, i, j))
					return true;
		return false;
	}

	// From ShapedRecipe
	private boolean matchesSpecific(CraftingInput inv, int p_77573_2_, int p_77573_3_) {
		NonNullList<Ingredient> ingredients = getIngredients();
		int width = getWidth();
		int height = getHeight();
		for (int i = 0; i < inv.width(); ++i) {
			for (int j = 0; j < inv.height(); ++j) {
				int k = i - p_77573_2_;
				int l = j - p_77573_3_;
				Ingredient ingredient = Ingredient.EMPTY;
				if (k >= 0 && l >= 0 && k < width && l < height)
					ingredient = ingredients.get(k + l * width);
				if (!ingredient.test(inv.getItem(i + j * inv.width())))
					return false;
			}
		}
		return true;
	}

	@Override
	public RecipeType<?> getType() {
		return AllRecipeTypes.MECHANICAL_CRAFTING.getType();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AllRecipeTypes.MECHANICAL_CRAFTING.getSerializer();
	}

	public boolean acceptsMirrored() {
		return acceptMirrored;
	}

	public static class Serializer extends ShapedRecipe.Serializer {
		
		private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(list -> {
            if (list.size() > 5) {
                return DataResult.error(() -> "Invalid pattern: too many rows, 5 is maximum");
            }
            if (list.isEmpty()) {
                return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
            }
            int i = ((String)list.get(0)).length();
            for (String string : list) {
                if (string.length() > 5) {
                    return DataResult.error(() -> "Invalid pattern: too many columns, 5 is maximum");
                }
                if (i == string.length()) continue;
                return DataResult.error(() -> "Invalid pattern: each row must be the same width");
            }
            return DataResult.success(list);
        }, Function.identity());
		
        private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(string -> {
            if (string.length() != 1) {
                return DataResult.error(() -> "Invalid key entry: '" + string + "' is an invalid symbol (must be 1 character only).");
            }
            if (" ".equals(string)) {
                return DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.");
            }
            return DataResult.success(Character.valueOf(string.charAt(0)));
        }, String::valueOf);
        
        public static final MapCodec<Data> MAP_CODEC = RecordCodecBuilder.mapCodec(
        		instance -> instance.group(
        				(ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC_NONEMPTY).fieldOf("key")).forGetter(data -> data.key()), 
        				(PATTERN_CODEC.fieldOf("pattern")).forGetter(data -> data.pattern())
        		).apply(instance, Data::new)
        );
   
        public static final MapCodec<ShapedRecipePattern> RESULT_CODEC = MAP_CODEC.flatXmap(ShapedRecipePattern::unpack, 
				shapedRecipePattern -> shapedRecipePattern.data.map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe"))
		);
        
		
		public static final MapCodec<ShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Codec.optionalField("group", Codec.STRING, false).forGetter(shapedRecipe -> Optional.of(shapedRecipe.getGroup())), 
						(CraftingBookCategory.CODEC.fieldOf("category")).orElse(CraftingBookCategory.MISC).forGetter(shapedRecipe -> shapedRecipe.category()), 
						RESULT_CODEC.forGetter(shapedRecipe -> shapedRecipe.pattern), 
						(ItemStack.CODEC.fieldOf("result")).forGetter(shapedRecipe -> shapedRecipe.getResultItem(null)), 
						Codec.optionalField("show_notification", Codec.BOOL, false).forGetter(shapedRecipe -> Optional.of(shapedRecipe.showNotification())),
						Codec.BOOL.fieldOf("acceptMirrored").forGetter(shapedRecipe -> false)
				).apply(instance, (group, cate, pat, stack, show, mirror) -> {
						ShapedRecipe rec = new ShapedRecipe(group.isEmpty() ? "undefined" : group.get(), cate, pat, stack, show.isEmpty() ? true : show.get());
						return fromShaped(rec, mirror);
				})
		);
		
		public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe>(){

			@Override
			public ShapedRecipe decode(RegistryFriendlyByteBuf buffer) {
				return fromShaped(ShapedRecipe.Serializer.STREAM_CODEC.decode(buffer), buffer.readBoolean() && buffer.readBoolean());
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buffer, ShapedRecipe recipe) {
				ShapedRecipe.Serializer.STREAM_CODEC.encode(buffer, recipe);
				if (recipe instanceof MechanicalCraftingRecipe) {
					buffer.writeBoolean(true);
					buffer.writeBoolean(((MechanicalCraftingRecipe) recipe).acceptsMirrored());
				} else {
					buffer.writeBoolean(false);
				}
			}
			
		};

		@Override
		public MapCodec<ShapedRecipe> codec() {
			return CODEC;
		}
		
		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe> streamCodec() {
			return STREAM_CODEC;
		}

//		@Override
//		public ShapedRecipe fromNetwork(FriendlyByteBuf buffer) {
//			return fromShaped(super.fromNetwork(buffer), buffer.readBoolean() && buffer.readBoolean());
//		}
//
//		@Override
//		public void toNetwork(FriendlyByteBuf p_199427_1_, ShapedRecipe p_199427_2_) {
//			super.toNetwork(p_199427_1_, p_199427_2_);
//			if (p_199427_2_ instanceof MechanicalCraftingRecipe) {
//				p_199427_1_.writeBoolean(true);
//				p_199427_1_.writeBoolean(((MechanicalCraftingRecipe) p_199427_2_).acceptsMirrored());
//			} else
//				p_199427_1_.writeBoolean(false);
//		}

	}

}
