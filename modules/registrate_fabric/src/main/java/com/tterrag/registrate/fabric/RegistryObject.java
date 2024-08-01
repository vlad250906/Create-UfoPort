package com.tterrag.registrate.fabric;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class RegistryObject<T> implements Supplier<T> {
	private static RegistryObject<?> EMPTY = new RegistryObject<>();

	private final ResourceLocation id;
	@Nullable
	private T object;

	private RegistryObject() {
		id = null;
	}

	@SuppressWarnings("unchecked")
	private RegistryObject(ResourceLocation id, Registry<? super T> registry) {
		Objects.requireNonNull(registry);
		this.id = id;
		object = (T) registry.get(this.id);
	}

	public static <T> RegistryObject<T> of(ResourceLocation id, Registry<? super T> registry) {
		return new RegistryObject<>(id, registry);
	}

	public static <T, U extends T> RegistryObject<U> create(final ResourceLocation name,
			final ResourceLocation registryName, String modid) {
		// noinspection unchecked
		return new RegistryObject<>(name, (Registry<U>) BuiltInRegistries.REGISTRY.get(registryName));
	}

	@SuppressWarnings("unchecked")
	public static <T> RegistryObject<T> empty() {
		return (RegistryObject<T>) EMPTY;
	}

	@Override
	@NotNull
	public T get() {
		Objects.requireNonNull(object);
		return object;
	}

	public ResourceLocation getId() {
		return id;
	}

	@SuppressWarnings("unchecked")
	public void updateReference(Registry<? super T> registry) {
		object = (T) registry.get(getId());
	}

	public boolean isPresent() {
		return object != null;
	}

	public void ifPresent(Consumer<? super T> consumer) {
		if (isPresent()) {
			consumer.accept(get());
		}
	}

	public Stream<T> stream() {
		return isPresent() ? Stream.of(get()) : Stream.of();
	}

	public RegistryObject<T> filter(Predicate<? super T> predicate) {
		Objects.requireNonNull(predicate);
		if (isPresent() && !predicate.test(get())) {
			return empty();
		}
		return this;
	}

	public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
		Objects.requireNonNull(mapper);
		if (isPresent()) {
			return Optional.ofNullable(mapper.apply(get()));
		}
		return Optional.empty();
	}

	public <U> Optional<U> flatMap(Function<? super T, Optional<U>> mapper) {
		Objects.requireNonNull(mapper);
		if (isPresent()) {
			return Objects.requireNonNull(mapper.apply(get()));
		}
		return Optional.empty();
	}

	public <U> Supplier<U> lazyMap(Function<? super T, ? extends U> mapper) {
		Objects.requireNonNull(mapper);
		return () -> isPresent() ? mapper.apply(get()) : null;
	}

	public T orElse(T other) {
		return isPresent() ? get() : other;
	}

	public T orElseGet(Supplier<? extends T> other) {
		return isPresent() ? get() : other.get();
	}

	public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		if (isPresent()) {
			return get();
		}
		throw exceptionSupplier.get();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof RegistryObject) {
			return Objects.equals(((RegistryObject<?>) obj).id, id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
