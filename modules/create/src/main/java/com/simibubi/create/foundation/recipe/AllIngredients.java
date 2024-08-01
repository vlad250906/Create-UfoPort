package com.simibubi.create.foundation.recipe;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;

public class AllIngredients {
	public static void register() {
		CustomIngredientSerializer.register(BlockTagIngredient.Serializer.INSTANCE);
	}
}
