package com.tterrag.registrate.fabric;

import java.util.Objects;
import java.util.function.Function;

public interface QuaFunction<T, U, P, R, S> {

    /**
     * Applies this function to the given arguments.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result
     */
    S apply(T t, U u, P p, R r);

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> QuaFunction<T, U, P, R, V> andThen(Function<? super S, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, U u, P p, R r) -> after.apply(apply(t, u, p, r));
    }
}