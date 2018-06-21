/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.rt;

import java.util.Map;

import java.util.function.Function;

@FunctionalInterface
public interface Action extends Function<Map<String, Object>, Object> {

}