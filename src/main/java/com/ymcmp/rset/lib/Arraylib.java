/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.lib;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public final class Arraylib {

    private Arraylib() {
        //
    }

    @Export("_array")
    public static List<Object> newArray(Object... data) {
        return new ArrayList<>(Arrays.asList(data));
    }

    @Export("_array_add")
    public static void arrayAdd(List<Object> l, Object... k) {
        l.addAll(Arrays.asList(k));
    }

    @Export("_array_fix")
    public static Object[] arrayFix(List<Object> l) {
        return l.toArray();
    }

    @Export("_sort")
    public static Object sort(Object k) {
        if (k == null) return null;

        // Do not sort in place
        if (k.getClass().isArray()) {
            final Object[] old = (Object[]) k;
            final Object[] mod = Arrays.copyOf(old, old.length);
            Arrays.sort(mod);
            return mod;
        }
        if (k instanceof Collection) {
            final ArrayList list = new ArrayList((Collection<?>) k);
            Collections.sort(list);
            return list;
        }
        if (k instanceof CharSequence) {
            final int[] arr = ((CharSequence) k).chars()
                    .sorted()
                    .toArray();
            return new String(arr, 0, arr.length);
        }
        if (k instanceof Stream) {
            return ((Stream<?>) k).sorted();
        }

        throw new RuntimeException("Type '" + k.getClass().getSimpleName() + "' cannot be sorted!");
    }

    @Export("_rev")
    public static Object reverse(Object k) {
        if (k == null) return null;

        // Do not reverse in place
        if (k.getClass().isArray()) {
            final Object[] old = (Object[]) k;
            final List<Object> mod = Arrays.asList(Arrays.copyOf(old, old.length));
            Collections.reverse(mod);
            return mod.toArray();
        }
        if (k instanceof Collection) {
            final ArrayList<?> list = new ArrayList((Collection<?>) k);
            Collections.reverse(list);
            return list;
        }
        if (k instanceof CharSequence) {
            return new StringBuilder((CharSequence) k)
                    .reverse()
                    .toString();
        }

        throw new RuntimeException("Type '" + k.getClass().getSimpleName() + "' cannot be reversed!");
    }

    @Export("_iota")
    public static Integer[] iota(int k) {
        return IntStream.rangeClosed(1, k)
                .mapToObj(e -> e)
                .toArray(Integer[]::new);
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
            ((Iterable<?>) x).forEach(el -> flattenHelper(col, el));
        } else if (x instanceof Stream) {
            ((Stream<?>) x).forEach(el -> flattenHelper(col, el));
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

    @Export("_len")
    public static Number length(final Object xs) {
        if (xs == null) return 0;

        if (xs.getClass().isArray()) {
            return ((Object[]) xs).length;
        }
        if (xs instanceof Collection) {
            return ((Collection<?>) xs).size();
        }
        if (xs instanceof Stream) {
            return ((Stream<?>) xs).count();
        }
        if (xs instanceof Map) {
            return ((Map<?, ?>) xs).size();
        }
        if (xs instanceof Map.Entry) {
            return 2;
        }
        if (xs instanceof CharSequence) {
            return ((CharSequence) xs).length();
        }
        return -1;
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
                        .skip(((Number) offset).longValue())
                        .findFirst()
                        .orElse(null);
            }
            if (base instanceof Stream) {
                return ((Stream<?>) base)
                        .skip(((Number) offset).longValue())
                        .findFirst()
                        .orElse(null);
            }

            throw new RuntimeException("Type '" + base.getClass().getSimpleName() + "' cannot be indexed!");
        } catch (IndexOutOfBoundsException | ClassCastException ex) {
            return null;
        }
    }

    public static Iterable<?> toIterable(final Object obj) {
        if (obj == null) return null;

        if (obj.getClass().isArray()) {
            return Arrays.asList((Object[]) obj);
        }
        if (obj instanceof Map<?, ?>) {
            return ((Map<?, ?>) obj).entrySet();
        }
        return (Iterable<?>) obj;
    }
}