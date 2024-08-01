package com.tterrag.registrate.fabric;

import java.util.function.Supplier;

// Copy of Minecraft's Lazy. Needed for NonNullLazyValue to work correctly with the remapper.
public class Lazy<T> {
	private Supplier<T> supplier;
	private T value;

	public Lazy(Supplier<T> delegate) {
		this.supplier = delegate;
	}

	public T get() {
		Supplier<T> supplier = this.supplier;
		if (supplier != null) {
			this.value = supplier.get();
			this.supplier = null;
		}

		return this.value;
	}
}
