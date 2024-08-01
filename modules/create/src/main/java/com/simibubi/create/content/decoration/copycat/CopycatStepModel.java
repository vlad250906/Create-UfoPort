package com.simibubi.create.content.decoration.copycat;

import java.util.function.Supplier;

import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.utility.Iterate;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CopycatStepModel extends CopycatModel {

	protected static final Vec3 VEC_Y_3 = new Vec3(0, .75, 0);
	protected static final Vec3 VEC_Y_2 = new Vec3(0, .5, 0);
	protected static final Vec3 VEC_Y_N2 = new Vec3(0, -.5, 0);
	protected static final AABB CUBE_AABB = new AABB(BlockPos.ZERO);

	public CopycatStepModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	protected void emitBlockQuadsInner(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
			Supplier<RandomSource> randomSupplier, RenderContext context, BlockState material,
			CullFaceRemovalData cullFaceRemovalData, OcclusionData occlusionData) {
		Direction facing = state.getOptionalValue(CopycatStepBlock.FACING).orElse(Direction.SOUTH);
		boolean upperHalf = state.getOptionalValue(CopycatStepBlock.HALF).orElse(Half.BOTTOM) == Half.TOP;

		BakedModel model = getModelOf(material);

		Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
		Vec3 normalScaled2 = normal.scale(.5);
		Vec3 normalScaledN3 = normal.scale(-.75);
		AABB bb = CUBE_AABB.contract(-normal.x * .75, .75, -normal.z * .75);

		SpriteFinder spriteFinder = SpriteFinder
				.get(Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS));

		// Use a mesh to defer quad emission since quads cannot be emitted inside a
		// transform
		MeshBuilder meshBuilder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
		QuadEmitter emitter = meshBuilder.getEmitter();
		context.pushTransform(quad -> {
			if (cullFaceRemovalData.shouldRemove(quad.cullFace())) {
				quad.cullFace(null);
			} else if (occlusionData.isOccluded(quad.cullFace())) {
				// Add quad to mesh and do not render original quad to preserve quad render
				// order
				// copyTo does not copy the material
				RenderMaterial quadMaterial = quad.material();
				quad.copyTo(emitter);
				emitter.material(quadMaterial);
				emitter.emit();
				return false;
			}

			// 4 Pieces
			for (boolean top : Iterate.trueAndFalse) {
				for (boolean front : Iterate.trueAndFalse) {

					AABB bb1 = bb;
					if (front)
						bb1 = bb1.move(normalScaledN3);
					if (top)
						bb1 = bb1.move(VEC_Y_3);

					Vec3 offset = Vec3.ZERO;
					if (front)
						offset = offset.add(normalScaled2);
					if (top != upperHalf)
						offset = offset.add(upperHalf ? VEC_Y_2 : VEC_Y_N2);

					Direction direction = quad.lightFace();

					if (front && direction == facing)
						continue;
					if (!front && direction == facing.getOpposite())
						continue;
					if (!top && direction == Direction.UP)
						continue;
					if (top && direction == Direction.DOWN)
						continue;

					// copyTo does not copy the material
					RenderMaterial quadMaterial = quad.material();
					quad.copyTo(emitter);
					emitter.material(quadMaterial);
					BakedModelHelper.cropAndMove(emitter, spriteFinder.find(emitter, 0), bb1, offset);
					emitter.emit();
				}
			}

			return false;
		});
		((FabricBakedModel) model).emitBlockQuads(blockView, material, pos, randomSupplier, context);
		context.popTransform();
		context.meshConsumer().accept(meshBuilder.build());
	}

}
