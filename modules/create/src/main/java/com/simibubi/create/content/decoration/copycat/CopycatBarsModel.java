package com.simibubi.create.content.decoration.copycat;

import java.util.function.Supplier;

import com.simibubi.create.foundation.block.render.SpriteShiftEntry;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class CopycatBarsModel extends CopycatModel {

	public CopycatBarsModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	public boolean useAmbientOcclusion() {
		return false;
	}

	@Override
	protected void emitBlockQuadsInner(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context, BlockState material, CullFaceRemovalData cullFaceRemovalData, OcclusionData occlusionData) {
		BakedModel model = getModelOf(material);
		TextureAtlasSprite mainTargetSprite = model.getParticleIcon();

		boolean vertical = state.getValue(CopycatPanelBlock.FACING)
			.getAxis() == Axis.Y;

		SpriteFinder spriteFinder = SpriteFinder.get(Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS));

		// This is very cursed
		SpriteAndBool altTargetSpriteHolder = new SpriteAndBool(mainTargetSprite, true);
		context.pushTransform(quad -> {
			if (altTargetSpriteHolder.bool && quad.cullFace() == null && quad.lightFace() == Direction.UP) {
				altTargetSpriteHolder.sprite = spriteFinder.find(quad, 0);
				altTargetSpriteHolder.bool = false;
			}
			return false;
		});
		((FabricBakedModel) model).emitBlockQuads(blockView, material, pos, randomSupplier, context);
		context.popTransform();
		TextureAtlasSprite altTargetSprite = altTargetSpriteHolder.sprite;

		context.pushTransform(quad -> {
			TextureAtlasSprite targetSprite;
			Direction cullFace = quad.cullFace();
			if (cullFace != null && (vertical || cullFace.getAxis() == Axis.Y)) {
				targetSprite = altTargetSprite;
			} else {
				targetSprite = mainTargetSprite;
			}

			TextureAtlasSprite original = spriteFinder.find(quad, 0);
			for (int vertex = 0; vertex < 4; vertex++) {
				float u = targetSprite.getU(SpriteShiftEntry.getUnInterpolatedU(original, quad.spriteU(vertex, 0)));
				float v = targetSprite.getV(SpriteShiftEntry.getUnInterpolatedV(original, quad.spriteV(vertex, 0)));
				quad.sprite(vertex, 0, u, v);
			}
			return true;
		});
		((FabricBakedModel) wrapped).emitBlockQuads(blockView, state, pos, randomSupplier, context);
		context.popTransform();
	}

	private static class SpriteAndBool {
		public TextureAtlasSprite sprite;
		public boolean bool;

		public SpriteAndBool(TextureAtlasSprite sprite, boolean bool) {
			this.sprite = sprite;
			this.bool = bool;
		}
	}

}
