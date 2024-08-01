package com.jozufozu.flywheel.backend.instancing;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import com.jozufozu.flywheel.backend.model.BufferBuilderExtension;
import com.jozufozu.flywheel.backend.model.DirectVertexConsumer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;

import net.minecraft.client.renderer.RenderType;

public class BatchDrawingTracker {

	protected final Set<RenderType> activeTypes = new HashSet<>();
	private final ByteBufferBuilder scratch;

	public BatchDrawingTracker() {
		scratch = new ByteBufferBuilder(8);
	}

	/**
	 * Get a direct vertex consumer for drawing the given number of vertices to the given RenderType.
	 * @param renderType The RenderType to draw to.
	 * @param vertexCount The number of vertices that will be drawn.
	 * @return A direct vertex consumer.
	 */
	public DirectVertexConsumer getDirectConsumer(RenderType renderType, int vertexCount) {
		activeTypes.add(renderType);
		return RenderTypeExtension.getDrawBuffer(renderType)
				.begin(vertexCount);
	}

	/**
	 * Draws all active DrawBuffers and reset them.
	 */
	public void endBatch() {
		// TODO: when/if this causes trouble with shaders, try to inject our BufferBuilders
		//  into the RenderBuffers from context.

		for (RenderType renderType : activeTypes) {
			_draw(renderType);
		}

		activeTypes.clear();
	}

	/**
	 * Draw and reset the DrawBuffer for the given RenderType.
	 * @param renderType The RenderType to draw.
	 */
	public void endBatch(RenderType renderType) {
		_draw(renderType);

		activeTypes.remove(renderType);
	}

	/**
	 * Resets all DrawBuffers to 0 vertices.
	 */
	public void clear() {
		for (RenderType type : activeTypes) {
			RenderTypeExtension.getDrawBuffer(type)
					.reset();
		}
		activeTypes.clear();
	}

	private void _draw(RenderType renderType) {
		DrawBuffer drawBuffer = RenderTypeExtension.getDrawBuffer(renderType);
		BufferBuilder scratch = new BufferBuilder(this.scratch, renderType.mode(), renderType.format());
		((BufferBuilderExtension) scratch).flywheel$freeBuffer();

		BufferBuilderExtension scratch2 = (BufferBuilderExtension) scratch;
		if (drawBuffer.hasVertices()) {
			ByteBufferBuilder builder = drawBuffer.inject(scratch2);
			
			MeshData mesh = scratch.build();
			mesh.sortQuads(builder, VertexSorting.DISTANCE_TO_ORIGIN);
			
//			ByteBuffer verts = mesh.vertexBuffer();
//			VertexFormat form = mesh.drawState().format();
//			LogUtils.getLogger().info(form.toString());
//			for(int i=0;i<verts.remaining();i+=36) {
//				String result = "";
//				result += (i / 36) + ")    ";
//				result += "Position: "+verts.getFloat()+"; "+verts.getFloat()+"; "+verts.getFloat() + "   ***   ";
//				result += "Color: "+((int)verts.get() + 128)+"; "+((int)verts.get() + 128)+"; "+((int)verts.get() + 128)+"; "+((int)verts.get() + 128) + "   ***   ";
//				result += "UV0: "+verts.getFloat()+"; "+verts.getFloat() + "   ***   ";
//				result += "UV1: "+verts.getShort()+"; "+verts.getShort() + "   ***   ";
//				result += "UV2: "+verts.getShort()+"; "+verts.getShort() + "   ***   ";
//				result += "Normal: "+((int)verts.get())+"; "+((int)verts.get())+"; "+((int)verts.get()) + "   ***   ";
//				LogUtils.getLogger().info(result);
//			}
			
			renderType.draw(mesh /*, VertexSorting.DISTANCE_TO_ORIGIN*/);

			drawBuffer.reset();
		}
	}

}
