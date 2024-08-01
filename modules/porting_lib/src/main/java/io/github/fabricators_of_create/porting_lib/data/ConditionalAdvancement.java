package io.github.fabricators_of_create.porting_lib.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.util.GsonHelper;

public class ConditionalAdvancement {
	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder {
		private List<ResourceCondition[]> conditions = new ArrayList<>();
		private List<Supplier<JsonElement>> advancements = new ArrayList<>();

		private List<ResourceCondition> currentConditions = new ArrayList<>();
		private boolean locked = false;

		public Builder addCondition(ResourceCondition condition) {
			if (locked)
				throw new IllegalStateException("Attempted to modify finished builder");
			currentConditions.add(condition);
			return this;
		}

		public Builder addAdvancement(Supplier<Advancement.Builder> callable) {
			if (locked)
				throw new IllegalStateException("Attempted to modify finished builder");
			addAdvancement(callable.get());
			return this;
		}

		public Builder addAdvancement(Advancement.Builder advancement) {
			return addAdvancement();
		}

		public Builder addAdvancement(RecipeOutput fromRecipe) {
			return addAdvancement(fromRecipe::advancement);
		}

		private Builder addAdvancement() {
			if (locked)
				throw new IllegalStateException("Attempted to modify finished builder");
			if (currentConditions.isEmpty())
				throw new IllegalStateException("Can not add a advancement with no conditions.");
			conditions.add(currentConditions.toArray(new ResourceCondition[currentConditions.size()]));
			//advancements.add(jsonSupplier);
			currentConditions.clear();
			return this;
		}

		public JsonObject write() {
			if (!locked) {
				if (!currentConditions.isEmpty())
					throw new IllegalStateException("Invalid builder state: Orphaned conditions");
				if (advancements.isEmpty())
					throw new IllegalStateException("Invalid builder state: No Advancements");
				locked = true;
			}
			JsonObject json = new JsonObject();
			JsonArray array = new JsonArray();
			json.add("advancements", array);
			for (int x = 0; x < conditions.size(); x++)
			{
				JsonObject holder = new JsonObject();

				JsonArray conds = new JsonArray();
				for (ResourceCondition c : conditions.get(x))
					conds.add(ResourceCondition.CODEC.encode(c, JsonOps.INSTANCE, null).getOrThrow());
				holder.add(ResourceConditions.CONDITIONS_KEY, conds);
				holder.add("advancement", advancements.get(x).get());

				array.add(holder);
			}
			return json;
		}
	}
}
