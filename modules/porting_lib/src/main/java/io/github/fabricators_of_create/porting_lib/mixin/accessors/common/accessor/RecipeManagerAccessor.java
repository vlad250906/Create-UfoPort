package io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor {
	//@Accessor("recipes")
	//Map<RecipeType<?>, Map<ResourceLocation, RecipeHolder<?>>> port_lib$getRecipes();

	@Invoker("byType")
	<I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> port_lib$byType(RecipeType<T> recipeType);
}
