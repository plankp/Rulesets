/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.function;

import java.util.function.Function;

@FunctionalInterface
public interface TriConsumer<T, U, V> {

    public void accept(T t, U u, V v);
}