package com.simibubi.create.content.kinetics.deployer;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

public class ItemApplicationRecipe extends ProcessingRecipe<RecipeInput> {

	public boolean keepHeldItem;

	public ItemApplicationRecipe(AllRecipeTypes type, ProcessingRecipeParams params) {
		super(type, params);
		keepHeldItem = params.keepHeldItem;
	}

	@Override
	public boolean matches(RecipeInput inv, Level p_77569_2_) {
		return ingredients.get(0)
			.test(inv.getItem(0))
			&& ingredients.get(1)
				.test(inv.getItem(1));
	}

	@Override
	protected int getMaxInputCount() {
		return 2;
	}

	@Override
	protected int getMaxOutputCount() {
		return 4;
	}

	public boolean shouldKeepHeldItem() {
		return keepHeldItem;
	}

	public Ingredient getRequiredHeldItem() {
		if (ingredients.isEmpty())
			throw new IllegalStateException("Item Application Recipe: " + id.toString() + " has no tool!");
		return ingredients.get(1);
	}

	public Ingredient getProcessedItem() {
		if (ingredients.size() < 2)
			throw new IllegalStateException("Item Application Recipe: " + id.toString() + " has no ingredient!");
		return ingredients.get(0);
	}
	
	public static ProcessingCodecBuilder getCodecIA() {
		ProcessingCodecBuilder pcb = ProcessingCodecBuilder.getDefaultBuilder();
		return pcb.addField(Codec.BOOL.optionalFieldOf("keepHeldItem").forGetter((O) -> (Optional)pcb.getGetterList(O).get(4)));
	}

	@Override
	public void readAdditional(List<Object> args) {
		super.readAdditional(args);
		keepHeldItem = false;
		if(((Optional)args.get(0)).isPresent()) 
			keepHeldItem = (Boolean)((Optional)args.get(0)).get();
		//keepHeldItem = GsonHelper.getAsBoolean(json, "keepHeldItem", false);
	}

	@Override
	public void writeAdditional(List<Object> args) {
		super.writeAdditional(args);
		args.add(keepHeldItem ? Optional.of(keepHeldItem) : Optional.empty());
	}

	@Override
	public void readAdditional(FriendlyByteBuf buffer) {
		super.readAdditional(buffer);
		keepHeldItem = buffer.readBoolean();
	}

	@Override
	public void writeAdditional(FriendlyByteBuf buffer) {
		super.writeAdditional(buffer);
		buffer.writeBoolean(keepHeldItem);
	}

}
