package com.simibubi.create.content.redstone.diodes;

import java.util.Vector;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;

import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile.ExistingModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.block.BlockModelProvider;
import io.github.fabricators_of_create.porting_lib.models.generators.item.ItemModelBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractDiodeGenerator extends SpecialBlockStateGen {

	private Vector<ModelFile> models;

	public static <I extends BlockItem> void diodeItemModel(DataGenContext<Item, I> c, RegistrateItemModelProvider p) {
		String name = c.getName();
		String path = "block/diodes/";
		ItemModelBuilder builder = p.withExistingParent(name, p.modLoc(path + name));
		builder.texture("top", path + name + "/item");
	}

	@Override
	protected final int getXRotation(BlockState state) {
		return 0;
	}

	@Override
	protected final int getYRotation(BlockState state) {
		return horizontalAngle(state.getValue(AbstractDiodeBlock.FACING));
	}

	protected abstract <T extends Block> Vector<ModelFile> createModels(DataGenContext<Block, T> ctx,
		BlockModelProvider prov);

	protected abstract int getModelIndex(BlockState state);

	@Override
	public final <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
		BlockState state) {
		if (models == null)
			models = createModels(ctx, prov.models());
		return models.get(getModelIndex(state));
	}

	protected Vector<ModelFile> makeVector(int size) {
		return new Vector<>(size);
	}

	protected ExistingModelFile existingModel(BlockModelProvider prov, String name) {
		return prov.getExistingFile(existing(name));
	}

	protected ResourceLocation existing(String name) {
		return Create.asResource("block/diodes/" + name);
	}

	protected <T extends Block> ResourceLocation texture(DataGenContext<Block, T> ctx, String name) {
		return Create.asResource("block/diodes/" + ctx.getName() + "/" + name);
	}

	protected ResourceLocation poweredTorch() {
		return ResourceLocation.parse("block/redstone_torch");
	}

}
