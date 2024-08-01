package com.simibubi.create.foundation.utility.fabric;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An Iterator wrapper that can apply a function to each entry of the wrapped iterator.
 * @param wrapped Iterator to wrap
 * @param process Function to apply to all entries. Can be used to replace or wrap entries themselves.
 */
public record ProcessingIterator<T>(Iterator<T> wrapped, Function<T, T> process) implements Iterator<T> {
	@Override
	public boolean hasNext() {
		return wrapped.hasNext();
	}

	@Override
	public T next() {
		return process.apply(wrapped.next());
	}

	@Override
	public void remove() {
		wrapped.remove();
	}

	@Override
	public void forEachRemaining(Consumer<? super T> action) {
		wrapped.forEachRemaining(t -> action.accept(process.apply(t)));
	}
}
