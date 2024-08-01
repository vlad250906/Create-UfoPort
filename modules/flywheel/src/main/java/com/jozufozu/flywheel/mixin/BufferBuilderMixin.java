package com.jozufozu.flywheel.mixin;

import java.nio.ByteBuffer;

import javax.annotation.Nonnull;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.backend.model.BufferBuilderExtension;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;

@Mixin(value = BufferBuilder.class, priority = 1500)
public abstract class BufferBuilderMixin implements BufferBuilderExtension {
	@Shadow
	private ByteBufferBuilder buffer;

	@Shadow
	private int vertices;

	@Shadow
	private VertexFormat format;

	@Shadow
	private VertexFormat.Mode mode;

	@Shadow
	private boolean building;
	
	@Shadow
	private long vertexPointer;
	
	@Shadow
	@Final
	private int[] offsetsByElement;

//	@Shadow
//	private void ensureCapacity(int increaseAmount) {
//	}

	@Override
	public int flywheel$getVertices() {
		return vertices;
	}
	
	@Override
	public VertexFormat flywheel$getFormat() {
		return format;
	}
	
	@Override
	public VertexFormat.Mode flywheel$getMode() {
		return mode;
	}

	@Override
	public void flywheel$freeBuffer() {
		//LogUtils.getLogger().info("flywheel$freeBuffer");
		if (this.buffer != null) {
			buffer.close();
			//MemoryUtil.memFree(this.buffer);
			this.buffer = null;
		}
	}

	@Override
	public void flywheel$injectForRender(@Nonnull ByteBufferBuilder buffer, @Nonnull VertexFormat format, int vertexCount) {
		this.building = true;
		this.mode = VertexFormat.Mode.QUADS;

		this.buffer = buffer;
		this.format = format;
		this.vertices = vertexCount;
		
		this.vertexPointer = (this.vertices - 1) * format.getVertexSize();
		if(this.vertexPointer < 0)
			this.vertexPointer = -1;
		
		//LogUtils.getLogger().info("Injecting for render");

//		this.currentElement = this.format.getElements().get(0);
//		this.elementIndex = 0;
	}

	@Override
	public void flywheel$appendBufferUnsafe(ByteBuffer buffer) {
		//LogUtils.getLogger().info("flywheel$appendBufferUnsafe");
		
		//LogUtils.getLogger().info("Append buffer unsafe");
		
		if (!building) {
			throw new IllegalStateException("BufferBuilder not started");
		}
//		if (elementIndex != 0) {
//			throw new IllegalStateException("Cannot append buffer while building vertex");
//		}

		int numBytes = buffer.remaining();
		if (numBytes % format.getVertexSize() != 0) {
			throw new IllegalArgumentException("Cannot append buffer with non-whole number of vertices");
		}
		int numVertices = numBytes / format.getVertexSize();

		//ensureCapacity(numBytes + format.getVertexSize());
		
//		this.buffer.position(nextElementByte);
//		MemoryUtil.memCopy(buffer, this.buffer);
//		this.buffer.position(originalPosition);
		
		long pntr = this.buffer.reserve(buffer.remaining());
		MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), pntr, numBytes);

//		nextElementByte += numBytes;
		vertices += numVertices;
		vertexPointer = (vertices - 1) * format.getVertexSize();
		if(vertexPointer < 0)
			vertexPointer = -1;
		
	}
	
//	@Inject(
//			method = "endLastVertex()V",
//			at = @At("HEAD")
//	)
//	private void flywheel$beforeNext(CallbackInfo ci) {
//		ByteBufferBuilderAccessor bbba = (ByteBufferBuilderAccessor)buffer;
//		long begin = bbba.flywheel$getPointer();
//		long capacity = bbba.flywheel$getCapacity();
//		//LogUtils.getLogger().info("flywheel$beforeNext: this = "+this+"; buffer = "+buffer+"; vertices = "+this.vertices+"; begin = "+Long.toHexString(begin)+"; addr = "+Long.toHexString(this.vertexPointer)+"; end = "+Long.toHexString(begin+capacity)+"; capacity = "+capacity);
//	}
	
//	@Redirect(
//			method = "fillExtendedData", 
//			at = @At(
//					value = "INVOKE",
//					target = "Lnet/irisshaders/iris/vertices/BufferBuilderPolygonView;setup([JII)V"
//			)
//	)
//	private void flywheel$fillExtendedData(BufferBuilderPolygonView polygon, long[] vertexPointers, int stride, int vertexAmount) {
//		ByteBufferBuilderAccessor bbba = (ByteBufferBuilderAccessor)buffer;
//		long begin = bbba.flywheel$getPointer();
//		long capacity = bbba.flywheel$getCapacity();
//		//LogUtils.getLogger().info("flywheel$fillExtendedData: this = "+this+"; buffer = "+buffer+"; begin = "+Long.toHexString(begin));
//		
//		polygon.setup(vertexPointers, stride, vertexAmount);
//
//		int midTexOffset = this.offsetsByElement[IrisVertexFormats.MID_TEXTURE_ELEMENT.id()];
//		int normalOffset = this.offsetsByElement[VertexFormatElement.NORMAL.id()];
//		int tangentOffset = this.offsetsByElement[IrisVertexFormats.TANGENT_ELEMENT.id()];
//		
//		String extraData = "vertexAmount = "+vertexAmount+"; realVerticesAmount = "+this.vertices+"; vertexPointers = ["+Long.toHexString(vertexPointers[0])+"; "+Long.toHexString(vertexPointers[1])+"; "+Long.toHexString(vertexPointers[2])+"; "+Long.toHexString(vertexPointers[3])+"; "+"]; midTexOffset = "+midTexOffset+"; tangentOffset = "+tangentOffset+"; normalOffset = "+normalOffset;
//		
//		if (vertexAmount == 3) {
//			for (int vertex = 0; vertex < vertexAmount; vertex++) {
//				assertAddress(vertexPointers[vertex] + midTexOffset, "<type = Triangle 1; "+extraData+">");
//				assertAddress(vertexPointers[vertex] + midTexOffset + 4, "<type = Triangle 2; "+extraData+">");
//				assertAddress(vertexPointers[vertex] + tangentOffset, "<type = Triangle 3; "+extraData+">");
//			}
//		} else {
//			for (int vertex = 0; vertex < vertexAmount; vertex++) {
//				assertAddress(vertexPointers[vertex] + midTexOffset, "<type = Polygon 1; "+extraData+">");
//				assertAddress(vertexPointers[vertex] + midTexOffset + 4, "<type = Polygon 2; "+extraData+">");
//				assertAddress(vertexPointers[vertex] + normalOffset, "<type = Polygon 3; "+extraData+">");
//				assertAddress(vertexPointers[vertex] + tangentOffset, "<type = Polygon 4; "+extraData+">");
//			}
//		}
//		
//	}
//	
//	@Unique
//	private void assertAddress(long addr, String message) {
//		ByteBufferBuilderAccessor bbba = (ByteBufferBuilderAccessor)buffer;
//		long begin = bbba.flywheel$getPointer();
//		long capacity = bbba.flywheel$getCapacity();
//		if(addr < begin || addr >= begin + capacity)
//			throw new IllegalStateException("EXCEPTION_ACCESS_VIOLATION: begin = "+Long.toHexString(begin)+"; addr = "+Long.toHexString(addr)+"; end = "+Long.toHexString(begin+capacity)+"; capacity = "+capacity+"; message = "+message);
//	}
	
	
}
