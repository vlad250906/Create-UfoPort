package com.simibubi.create.content.kinetics.deployer;

import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandlerContainer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

public class DeployerRecipeSearchEvent {
	private boolean canceled = false;
	private final DeployerBlockEntity blockEntity;
	private final ItemStackHandlerContainer inventory;
	@Nullable
	Recipe<? extends RecipeInput> recipe = null;
	private int maxPriority = 0;

	public static final Event<DeployerRecipeSearchCallback> EVENT = EventFactory.createArrayBacked(DeployerRecipeSearchCallback.class, callbacks -> (event) -> {
		for (DeployerRecipeSearchCallback callback : callbacks) {
			callback.handle(event);
		}
	});

	@FunctionalInterface
	public interface DeployerRecipeSearchCallback {
		void handle(DeployerRecipeSearchEvent event);
	}

	public DeployerRecipeSearchEvent(DeployerBlockEntity blockEntity, ItemStackHandlerContainer inventory) {
		this.blockEntity = blockEntity;
		this.inventory = inventory;
	}

//	@Override
//	public boolean isCancelable() {
//		return true;
//	}

	public void cancel() {
		canceled = true;
	}

	public DeployerBlockEntity getBlockEntity() {
		return blockEntity;
	}

	public ItemStackHandlerContainer getInventory() {
		return inventory;
	}

	// lazyness to not scan for recipes that aren't selected
	public boolean shouldAddRecipeWithPriority(int priority) {
		return !canceled && priority > maxPriority;
	}

	@Nullable
	public Recipe<? extends RecipeInput> getRecipe() {
		if (canceled)
			return null;
		return recipe;
	}

	public void addRecipe(Supplier<Optional<? extends Recipe<? extends RecipeInput>>> recipeSupplier, int priority) {
		if (!shouldAddRecipeWithPriority(priority))
			return;
		recipeSupplier.get().ifPresent(newRecipe -> {
			this.recipe = newRecipe;
			maxPriority = priority;
		});
	}
}
