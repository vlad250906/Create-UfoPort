package com.simibubi.create.content.fluids;

import java.util.Arrays;
import java.util.function.Supplier;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes.ComponentPartials;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Iterate;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class PipeAttachmentModel extends ForwardingBakedModel {

	public PipeAttachmentModel(BakedModel template) {
		wrapped = template;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter world, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
		PipeModelData data = new PipeModelData();
		BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);

		RenderAttachedBlockView attachmentView = (RenderAttachedBlockView) world;
		Object attachment = attachmentView.getBlockEntityRenderAttachment(pos);
		if (attachment instanceof AttachmentTypes[] attachments) {
			for (int i = 0; i < attachments.length; i++) {
				data.putAttachment(Iterate.directions[i], attachments[i]);
			}
		}

		if (bracket != null)
			data.putBracket(bracket.getBracket());

		data.setEncased(FluidPipeBlock.shouldDrawCasing(world, pos, state));

		super.emitBlockQuads(world, state, pos, randomSupplier, context);

		addQuads(world, state, pos, randomSupplier, context, data);
	}

	// fabric: unnecessary
	// TODO: Update once MinecraftForge#9163 is merged
//	@SuppressWarnings("removal")
//	@Override
//	public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
//		ChunkRenderTypeSet set = super.getRenderTypes(state, rand, data);
//		if (set.isEmpty()) {
//			return ItemBlockRenderTypes.getRenderLayers(state);
//		}
//		return set;
//	}

	private void addQuads(BlockAndTintGetter world, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context,
		PipeModelData pipeData) {
		BakedModel bracket = pipeData.getBracket();
		if (bracket != null)
			((FabricBakedModel) bracket).emitBlockQuads(world, state, pos, randomSupplier, context);
		for (Direction d : Iterate.directions) {
			AttachmentTypes type = pipeData.getAttachment(d);
			for (ComponentPartials partial : type.partials) {
				((FabricBakedModel) AllPartialModels.PIPE_ATTACHMENTS.get(partial)
					.get(d)
					.get())
					.emitBlockQuads(world, state, pos, randomSupplier, context);
			}
		}
		if (pipeData.isEncased())
			((FabricBakedModel) AllPartialModels.FLUID_PIPE_CASING.get())
				.emitBlockQuads(world, state, pos, randomSupplier, context);
	}

	private static class PipeModelData {
		private AttachmentTypes[] attachments;
		private boolean encased;
		private BakedModel bracket;

		public PipeModelData() {
			attachments = new AttachmentTypes[6];
			Arrays.fill(attachments, AttachmentTypes.NONE);
		}

		public void putBracket(BlockState state) {
			if (state != null) {
				this.bracket = Minecraft.getInstance()
					.getBlockRenderer()
					.getBlockModel(state);
			}
		}

		public BakedModel getBracket() {
			return bracket;
		}

		public void putAttachment(Direction face, AttachmentTypes rim) {
			attachments[face.get3DDataValue()] = rim;
		}

		public AttachmentTypes getAttachment(Direction face) {
			return attachments[face.get3DDataValue()];
		}

		public void setEncased(boolean encased) {
			this.encased = encased;
		}

		public boolean isEncased() {
			return encased;
		}
	}

}
