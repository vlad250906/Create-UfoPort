package com.simibubi.create.content.decoration.girder;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.utility.Iterate;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class ConnectedGirderModel extends CTModel {

	public ConnectedGirderModel(BakedModel originalModel) {
		super(originalModel, new GirderCTBehaviour());
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
		ConnectionData data = new ConnectionData();
		for (Direction d : Iterate.horizontalDirections)
			data.setConnected(d, GirderBlock.isConnected(blockView, pos, state, d));

		super.emitBlockQuads(blockView, state, pos, randomSupplier, context);

		for (Direction d : Iterate.horizontalDirections)
			if (data.isConnected(d))
				((FabricBakedModel) AllPartialModels.METAL_GIRDER_BRACKETS.get(d)
					.get())
					.emitBlockQuads(blockView, state, pos, randomSupplier, context);
	}

	private static class ConnectionData {
		boolean[] connectedFaces;

		public ConnectionData() {
			connectedFaces = new boolean[4];
			Arrays.fill(connectedFaces, false);
		}

		void setConnected(Direction face, boolean connected) {
			connectedFaces[face.get2DDataValue()] = connected;
		}

		boolean isConnected(Direction face) {
			return connectedFaces[face.get2DDataValue()];
		}
	}

}
