package io.github.fabricators_of_create.porting_lib.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import io.github.fabricators_of_create.porting_lib.core.PortingLib;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ConditionalRecipe {
	public static final RecipeSerializer<Recipe<?>> SERIALZIER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, PortingLib.id("conditional"), new Serializer<>());

	public static Builder builder() {
		return new Builder();
	}

	public static class Serializer<T extends Recipe<?>> implements RecipeSerializer<T> {
		/*@SuppressWarnings("unchecked") // We return a nested one, so we can't know what type it is.
		@Override
		public T fromJson(ResourceLocation recipeId, JsonObject json) {
			JsonArray items = GsonHelper.getAsJsonArray(json, "recipes");
			int idx = 0;
			for (JsonElement ele : items) {
				if (!ele.isJsonObject())
					throw new JsonSyntaxException("Invalid recipes entry at index " + idx + " Must be JsonObject");
				if (processConditions(GsonHelper.getAsJsonArray(ele.getAsJsonObject(), ResourceConditions.CONDITIONS_KEY)))
					return (T)RecipeManager.fromJson(recipeId, GsonHelper.getAsJsonObject(ele.getAsJsonObject(), "recipe"));
				idx++;
			}
			return null;
		}

		public static boolean processConditions(JsonArray conditions) {
			for (int x = 0; x < conditions.size(); x++) {
				if (!conditions.get(x).isJsonObject())
					throw new JsonSyntaxException("Conditions must be an array of JsonObjects");

				JsonObject json = conditions.get(x).getAsJsonObject();
				if (!CraftingHelper.getConditionPredicate(json).test(json))
					return false;
			}
			return true;
		}
		
		@Override public T fromNetwork(FriendlyByteBuf buffer) { return null; }
		@Override public void toNetwork(FriendlyByteBuf buffer, T recipe) {}

		//Should never get here as we return one of the recipes we wrap.
		*/
		
		
		@Override
		public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
			return null;
		}

		@Override
		public MapCodec<T> codec() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public static class Builder {
		private List<ResourceCondition[]> conditions = new ArrayList<>();
		private List<RecipeOutput> recipes = new ArrayList<>();
		private ResourceLocation advId;
		private ConditionalAdvancement.Builder adv;

		private List<ResourceCondition> currentConditions = new ArrayList<>();

		public Builder addCondition(ResourceCondition condition) {
			currentConditions.add(condition);
			return this;
		}

		public Builder addRecipe(Consumer<Consumer<RecipeOutput>> callable) {
			callable.accept(this::addRecipe);
			return this;
		}

		public Builder addRecipe(RecipeOutput recipe) {
			if (currentConditions.isEmpty())
				throw new IllegalStateException("Can not add a recipe with no conditions.");
			conditions.add(currentConditions.toArray(new ResourceCondition[currentConditions.size()]));
			recipes.add(recipe);
			currentConditions.clear();
			return this;
		}

		public Builder generateAdvancement() {
			return generateAdvancement(null);
		}

		public Builder generateAdvancement(@Nullable ResourceLocation id) {
			ConditionalAdvancement.Builder builder = ConditionalAdvancement.builder();
			for(int i=0;i<recipes.size();i++) {
				for(ResourceCondition cond : conditions.get(i))
					builder = builder.addCondition(cond);
				builder = builder.addAdvancement(recipes.get(i));
			}
			return setAdvancement(id, builder);
		}

		public Builder setAdvancement(ConditionalAdvancement.Builder advancement) {
			return setAdvancement(null, advancement);
		}

		public Builder setAdvancement(String namespace, String path, ConditionalAdvancement.Builder advancement) {
			return setAdvancement(ResourceLocation.fromNamespaceAndPath(namespace, path), advancement);
		}

		public Builder setAdvancement(@Nullable ResourceLocation id, ConditionalAdvancement.Builder advancement) {
			if (this.adv != null)
				throw new IllegalStateException("Invalid ConditionalRecipeBuilder, Advancement already set");
			this.advId = id;
			this.adv = advancement;
			return this;
		}

		public void build(Consumer<RecipeOutput> consumer, String namespace, String path) {
			build(consumer, ResourceLocation.fromNamespaceAndPath(namespace, path));
		}

		public void build(Consumer<RecipeOutput> consumer, ResourceLocation id) {
			if (!currentConditions.isEmpty())
				throw new IllegalStateException("Invalid ConditionalRecipe builder, Orphaned conditions");
			if (recipes.isEmpty())
				throw new IllegalStateException("Invalid ConditionalRecipe builder, No recipes");

			if (advId == null && adv != null) {
				advId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "recipes/" + id.getPath());
			}

			consumer.accept(new Finished(id, conditions, recipes, advId, adv));
		}
	}

	private static class Finished implements RecipeOutput {
		private final ResourceLocation id;
		private final List<ResourceCondition[]> conditions;
		private final List<RecipeOutput> recipes;
		private final ResourceLocation advId;
		private final ConditionalAdvancement.Builder adv;

		private Finished(ResourceLocation id, List<ResourceCondition[]> conditions, List<RecipeOutput> recipes, @Nullable ResourceLocation advId, @Nullable ConditionalAdvancement.Builder adv) {
			this.id = id;
			this.conditions = conditions;
			this.recipes = recipes;
			this.advId = advId;
			this.adv = adv;
		}

		/*@Override
		public void serializeRecipeData(JsonObject json) {
			JsonArray array = new JsonArray();
			json.add("recipes", array);
			for (int x = 0; x < conditions.size(); x++) {
				JsonObject holder = new JsonObject();

				JsonArray conds = new JsonArray();
				for (ConditionJsonProvider c : conditions.get(x))
					conds.add(c.toJson());
				holder.add(ResourceConditions.CONDITIONS_KEY, conds);
				holder.add("recipe", recipes.get(x).serializeRecipe());

				array.add(holder);
			}
		}

		@Override
		public ResourceLocation getId() {
			return id;
		}

		@Override
		public RecipeSerializer<?> getType() {
			return SERIALZIER;
		}

		@Override
		public JsonObject serializeAdvancement() {
			return adv == null ? null : adv.write();
		}

		@Override
		public ResourceLocation getAdvancementId() {
			return advId;
		}*/

		@Override
		public void accept(ResourceLocation var1, Recipe<?> var2, AdvancementHolder var3) {
			
		}

		@Override
		public net.minecraft.advancements.Advancement.Builder advancement() {
			return null;
		}
	}

	public static void init() {}
}
