package com.tterrag.registrate.fabric;

import com.tterrag.registrate.util.nullness.NonnullType;

public interface NonNullQuaFunction<@NonnullType T, @NonnullType U, @NonnullType P, @NonnullType R, @NonnullType S> extends QuaFunction<T, U, P, R, S> {

    @Override
    S apply(T t, U u, P p, R r);
}