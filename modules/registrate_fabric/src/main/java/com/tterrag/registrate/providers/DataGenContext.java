package com.tterrag.registrate.providers;

import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonnullType;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * A context bean passed to data generator callbacks. Contains the entry that data is being created for, and some metadata about the entry.
 *
 * @param <R>
 *            Type of the registry to which the entry belongs
 * @param <E>
 *            Type of the object for which data is being generated
 */
public class DataGenContext<R, E extends R> implements NonNullSupplier<E> {

    private final NonNullSupplier<E> entry;
    private final String name;
    private final ResourceLocation id;

    public DataGenContext(NonNullSupplier<E> entry, String name, ResourceLocation id) {
		this.entry = entry;
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public ResourceLocation getId() {
		return id;
	}

	@SuppressWarnings("null")
    public @NonnullType E getEntry() {
        return entry.get();
    }
	
	@Override
	public @NonnullType E get() {
		// TODO Auto-generated method stub
		return entry.get();
	}
	
	@Override
	public NonNullSupplier<@NonnullType E> lazy() {
		return entry.lazy();
	}

    public static <R, E extends R> DataGenContext<R, E> from(Builder<R, E, ?, ?> builder, ResourceKey<? extends Registry<R>> type) {
        return new DataGenContext<R, E>(NonNullSupplier.of(builder.getOwner().<R, E>get(builder.getName(), type)), builder.getName(),
                ResourceLocation.fromNamespaceAndPath(builder.getOwner().getModid(), builder.getName()));
    }
}
