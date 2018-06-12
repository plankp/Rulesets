package com.ymcmp.rset.rt;

import java.util.Map;

import java.util.function.Function;

@FunctionalInterface
public interface Action extends Function<Map<String, Object>, Object> {

}