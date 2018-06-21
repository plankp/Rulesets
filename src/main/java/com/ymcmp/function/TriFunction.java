/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.function;

import java.util.function.Function;

@FunctionalInterface
public interface TriFunction<S, T, U, R> {

    public R apply(S s, T t, U u);

    public default <V> TriFunction<S, T, U, V> andThen(Function<? super R, ? extends V> after) {
        return (s, t, u) -> after.apply(apply(s, t, u));
    }
}