package com.simibubi.create.foundation.model;

import static com.simibubi.create.foundation.block.render.SpriteShiftEntry.getUnInterpolatedU;
import static com.simibubi.create.foundation.block.render.SpriteShiftEntry.getUnInterpolatedV;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BakedModelHelper {

	public static void cropAndMove(MutableQuadView quad, TextureAtlasSprite sprite, AABB crop, Vec3 move) {
		Vec3 xyz0 = BakedQuadHelper.getXYZ(quad, 0);
		Vec3 xyz1 = BakedQuadHelper.getXYZ(quad, 1);
		Vec3 xyz2 = BakedQuadHelper.getXYZ(quad, 2);
		Vec3 xyz3 = BakedQuadHelper.getXYZ(quad, 3);

		Vec3 uAxis = xyz3.add(xyz2)
			.scale(.5);
		Vec3 vAxis = xyz1.add(xyz2)
			.scale(.5);
		Vec3 center = xyz3.add(xyz2)
			.add(xyz0)
			.add(xyz1)
			.scale(.25);

		float u0 = quad.spriteU(0, 0);
		float u3 = quad.spriteU(3, 0);
		float v0 = quad.spriteV(0, 0);
		float v1 = quad.spriteV(1, 0);

		float uScale = (float) Math
			.round((getUnInterpolatedU(sprite, u3) - getUnInterpolatedU(sprite, u0)) / xyz3.distanceTo(xyz0));
		float vScale = (float) Math
			.round((getUnInterpolatedV(sprite, v1) - getUnInterpolatedV(sprite, v0)) / xyz1.distanceTo(xyz0));

		if (uScale == 0) {
			float v3 = quad.spriteV(3, 0);
			float u1 = quad.spriteU(1, 0);
			uAxis = xyz1.add(xyz2)
				.scale(.5);
			vAxis = xyz3.add(xyz2)
				.scale(.5);
			uScale = (float) Math
				.round((getUnInterpolatedU(sprite, u1) - getUnInterpolatedU(sprite, u0)) / xyz1.distanceTo(xyz0));
			vScale = (float) Math
				.round((getUnInterpolatedV(sprite, v3) - getUnInterpolatedV(sprite, v0)) / xyz3.distanceTo(xyz0));

		}

		uAxis = uAxis.subtract(center)
			.normalize();
		vAxis = vAxis.subtract(center)
			.normalize();

		Vec3 min = new Vec3(crop.minX, crop.minY, crop.minZ);
		Vec3 max = new Vec3(crop.maxX, crop.maxY, crop.maxZ);

		for (int vertex = 0; vertex < 4; vertex++) {
			Vec3 xyz = BakedQuadHelper.getXYZ(quad, vertex);
			Vec3 newXyz = VecHelper.componentMin(max, VecHelper.componentMax(xyz, min));
			Vec3 diff = newXyz.subtract(xyz);

			if (diff.lengthSqr() > 0) {
				float u = quad.spriteU(vertex, 0);
				float v = quad.spriteV(vertex, 0);
				float uDiff = (float) uAxis.dot(diff) * uScale;
				float vDiff = (float) vAxis.dot(diff) * vScale;
				quad.sprite(vertex, 0,
						sprite.getU(getUnInterpolatedU(sprite, u) + uDiff),
						sprite.getV(getUnInterpolatedV(sprite, v) + vDiff));
			}

			BakedQuadHelper.setXYZ(quad, vertex, newXyz.add(move));
		}
	}

	public static BakedModel generateModel(BakedModel template, UnaryOperator<TextureAtlasSprite> spriteSwapper) {
		RandomSource random = RandomSource.create();

		Map<Direction, List<BakedQuad>> culledFaces = new EnumMap<>(Direction.class);
		for (Direction cullFace : Iterate.directions) {
			random.setSeed(42L);
			List<BakedQuad> quads = template.getQuads(null, cullFace, random);
			culledFaces.put(cullFace, swapSprites(quads, spriteSwapper));
		}

		random.setSeed(42L);
		List<BakedQuad> quads = template.getQuads(null, null, random);
		List<BakedQuad> unculledFaces = swapSprites(quads, spriteSwapper);

		TextureAtlasSprite particleSprite = template.getParticleIcon();
		TextureAtlasSprite swappedParticleSprite = spriteSwapper.apply(particleSprite);
		if (swappedParticleSprite != null) {
			particleSprite = swappedParticleSprite;
		}
		return new SimpleBakedModel(unculledFaces, culledFaces, template.useAmbientOcclusion(), template.usesBlockLight(), template.isGui3d(), particleSprite, template.getTransforms(), ItemOverrides.EMPTY);
	}

	public static List<BakedQuad> swapSprites(List<BakedQuad> quads, UnaryOperator<TextureAtlasSprite> spriteSwapper) {
		List<BakedQuad> newQuads = new ArrayList<>(quads);
		int size = quads.size();
		for (int i = 0; i < size; i++) {
			BakedQuad quad = quads.get(i);
			TextureAtlasSprite sprite = quad.getSprite();
			TextureAtlasSprite newSprite = spriteSwapper.apply(sprite);
			if (newSprite == null || sprite == newSprite)
				continue;

			BakedQuad newQuad = BakedQuadHelper.clone(quad);
			int[] vertexData = newQuad.getVertices();

			for (int vertex = 0; vertex < 4; vertex++) {
				float u = BakedQuadHelper.getU(vertexData, vertex);
				float v = BakedQuadHelper.getV(vertexData, vertex);
				BakedQuadHelper.setU(vertexData, vertex, newSprite.getU(getUnInterpolatedU(sprite, u)));
				BakedQuadHelper.setV(vertexData, vertex, newSprite.getV(getUnInterpolatedV(sprite, v)));
			}

			newQuads.set(i, newQuad);
		}
		return newQuads;
	}
}
