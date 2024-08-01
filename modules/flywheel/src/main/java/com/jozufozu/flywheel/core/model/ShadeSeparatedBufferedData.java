package com.jozufozu.flywheel.core.model;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

import com.jozufozu.flywheel.util.FlwUtil;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;

public interface ShadeSeparatedBufferedData {
	ByteBuffer vertexBuffer();

	ByteBuffer indexBuffer();

	MeshData.DrawState drawState();
	
	MeshData meshData();

	int unshadedStartVertex();

	void release();

	static final class NativeImpl implements ShadeSeparatedBufferedData {
		private final ByteBuffer vertexBuffer;
		private final ByteBuffer indexBuffer;
		private final MeshData meshData;
		private final int unshadedStartVertex;

		public NativeImpl(ByteBuffer vertexBuffer, ByteBuffer indexBuffer, MeshData meshData, int unshadedStartVertex) {
			this.vertexBuffer = FlwUtil.copyBuffer(vertexBuffer);
			this.indexBuffer = FlwUtil.copyBuffer(indexBuffer);
			this.meshData = meshData;
			this.unshadedStartVertex = unshadedStartVertex;
		}

		@Override
		public ByteBuffer vertexBuffer() {
			return vertexBuffer;
		}

		@Override
		public ByteBuffer indexBuffer() {
			return indexBuffer;
		}

		@Override
		public MeshData.DrawState drawState() {
			return meshData.drawState();
		}

		@Override
		public int unshadedStartVertex() {
			return unshadedStartVertex;
		}

		@Override
		public void release() {
			MemoryUtil.memFree(vertexBuffer);
			MemoryUtil.memFree(indexBuffer);
		}

		@Override
		public MeshData meshData() {
			return meshData;
		}
	}
}
