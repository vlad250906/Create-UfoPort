package com.tterrag.registrate.fabric;

import com.tterrag.registrate.util.nullness.NonnullType;

@FunctionalInterface
public interface NonNullTriFunction<@NonnullType T, @NonnullType U, @NonnullType P, @NonnullType R> extends TriFunction<T, U, P, R> {

    @Override
    R apply(T t, U u, P p);
}