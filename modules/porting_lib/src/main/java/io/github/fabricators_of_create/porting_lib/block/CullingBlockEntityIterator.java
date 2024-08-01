package io.github.fabricators_of_create.porting_lib.block;

import java.util.Iterator;
import java.util.NoSuchElementException;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CullingBlockEntityIterator implements Iterator<BlockEntity> {
	private final Iterator<? extends BlockEntity> wrapped;
	private final Frustum frustum;

	private BlockEntity next;
	private boolean nextChecked;

	public CullingBlockEntityIterator(Iterator<? extends BlockEntity> iterator, Frustum frustum) {
		wrapped = iterator;
		this.frustum = frustum;
	}

	@Override
	public boolean hasNext() {
		ensureNextChecked();
		return next != null;
	}

	@Override
	public BlockEntity next() {
		ensureNextChecked();
		if (next == null) {
			throw new NoSuchElementException();
		}
		nextChecked = false;
		return next;
	}

	@Override
	public void remove() {
		wrapped.remove();
	}

	private void ensureNextChecked() {
		if (!nextChecked) {
			next = nextCulled();
			nextChecked = true;
		}
	}

	private BlockEntity nextCulled() {
		while (true) {
			if (wrapped.hasNext()) {
				Object next = wrapped.next();
				if (next instanceof CustomRenderBoundingBoxBlockEntity cullable) {
					if (frustum.isVisible(cullable.getRenderBoundingBox())) {
						return (BlockEntity)next;
					}
				} else {
					return (BlockEntity)next;
				}
			} else {
				return null;
			}
		}
	}
}
