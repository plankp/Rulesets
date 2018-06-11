package com.ymcmp.function;

import java.util.function.Function;

@FunctionalInterface
public interface TriFunction<S, T, U, R> {

    public R apply(S s, T t, U u);

    public default <V> TriFunction<S, T, U, V> andThen(Function<? super R, ? extends V> after) {
        return (s, t, u) -> after.apply(apply(s, t, u));
    }
}