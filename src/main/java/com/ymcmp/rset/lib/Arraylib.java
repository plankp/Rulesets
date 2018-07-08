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
import java.util.stream.Collectors;
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

    public static int polyArraylength(final Object k) {
        if (k instanceof byte[]) return ((byte[]) k).length;
        if (k instanceof short[]) return ((short[]) k).length;
        if (k instanceof char[]) return ((char[]) k).length;
        if (k instanceof int[]) return ((int[]) k).length;
        if (k instanceof float[]) return ((float[]) k).length;
        if (k instanceof long[]) return ((long[]) k).length;
        if (k instanceof double[]) return ((double[]) k).length;
        if (k instanceof Object[]) return ((Object[]) k).length;
        return -1;
    }

    public static Object polyAaload(final Object k, int offset) {
        if (k instanceof byte[]) return ((byte[]) k)[offset];
        if (k instanceof short[]) return ((short[]) k)[offset];
        if (k instanceof char[]) return ((char[]) k)[offset];
        if (k instanceof int[]) return ((int[]) k)[offset];
        if (k instanceof float[]) return ((float[]) k)[offset];
        if (k instanceof long[]) return ((long[]) k)[offset];
        if (k instanceof double[]) return ((double[]) k)[offset];
        if (k instanceof Object[]) return ((Object[]) k)[offset];
        throw new ClassCastException(k + " is not an array");
    }

    public static List<Object> polyArrayToList(final Object k) {
        final int upperBound = polyArraylength(k);
        if (upperBound < 0) return null;

        final List<Object> list = new ArrayList<>();
        for (int i = 0; i < upperBound; ++i) list.add(polyAaload(k, i));
        return list;
    }

    @Export("_sort")
    public static Object sort(Object k) {
        if (k == null) return null;

        // Do not sort in place
        final List mod = polyArrayToList(k);
        if (mod != null) {
            Collections.sort(mod);
            return mod.toArray();
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
        final List mod = polyArrayToList(k);
        if (mod != null) {
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
    public static int[] iota(int k) {
        return IntStream.rangeClosed(1, k).toArray();
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
            for (final Object el : polyArrayToList(x)) flattenHelper(col, el);
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

        final int k = polyArraylength(xs);
        if (k >= 0) return k;

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
                return polyAaload(base, ((Number) offset).intValue());
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

        final Iterable<?> it = polyArrayToList(obj);
        if (it != null) return it;

        if (obj instanceof Map<?, ?>) {
            return ((Map<?, ?>) obj).entrySet();
        }
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).chars().mapToObj(e -> (char) e).collect(Collectors.toList());
        }
        return (Iterable<?>) obj;
    }
}