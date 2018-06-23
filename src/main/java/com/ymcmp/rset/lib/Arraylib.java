/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.lib;

import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.StreamSupport;

public final class Arraylib {

    private Arraylib() {
        //
    }

    @Export("_flatten")
    public static Object[] flatten(final Object... xs) {
        final ArrayList<Object> list = new ArrayList<>();
        for (final Object x : xs) flattenHelper(list, x);
        return list.toArray();
    }

    private static void flattenHelper(final Collection<Object> col, final Object x) {
        if (x == null) {
            col.add(null);
        } else if (x.getClass().isArray()) {
            final Object[] arr = (Object[]) x;
            for (final Object el : arr) flattenHelper(col, el);
        } else if (x instanceof Iterable) {
            final Iterable<?> it = (Iterable<?>) x;
            for (final Object el : it) flattenHelper(col, el);
        } else if (x instanceof Map) {
            flattenHelper(col, ((Map<?, ?>) x).entrySet());
        } else if (x instanceof Map.Entry) {
            final Map.Entry<?, ?> ent = (Map.Entry<?, ?>) x;
            col.add(ent.getKey());
            col.add(ent.getValue());
        } else {
            col.add(x);
        }
    }

    @Export("_subs")
    public static Object subscript(final Object base, final Object offset) {
        if (base == null) return null;
        try {
            if (base.getClass().isArray()) {
                return ((Object[]) base)[((Number) offset).intValue()];
            }
            if (base instanceof CharSequence) {
                return ((CharSequence) base).charAt(((Number) offset).intValue());
            }
            if (base instanceof Collection) {
                return ((Collection<?>) base).toArray()[((Number) offset).intValue()];
            }
            if (base instanceof Map) {
                return ((Map<?, ?>) base).get(offset);
            }
            if (base instanceof Map.Entry) {
                final Map.Entry<?, ?> ent = (Map.Entry<?, ?>) base;
                switch (((Number) offset).intValue()) {
                    case 0:     return ent.getKey();
                    case 1:     return ent.getValue();
                    default:    return null;
                }
            }
            if (base instanceof Iterable) {
                return StreamSupport.stream(((Iterable<?>) base).spliterator(), false)
                        .skip(((Number) offset).intValue())
                        .findFirst()
                        .orElse(null);
            }

            throw new RuntimeException("Type '" + base.getClass().getSimpleName() + "' cannot be indexed!");
        } catch (IndexOutOfBoundsException | ClassCastException ex) {
            return null;
        }
    }
}