/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.lib;

import java.util.Map;
import java.util.Arrays;
import java.util.Objects;
import java.util.Collection;
import java.util.stream.Stream;

public final class Stdlib {

    @Export public static final Object _null = null;

    private Stdlib() {
        // Do nothing
    }

    @Export("_ord")
    public static Object[] toOrdinal(final Object... args) {
        for (int i = 0; i < args.length; ++i) {
            final Object k = args[i];
            if (k instanceof Character) {
                args[i] = (int) ((Character) k).charValue();
            } else if (k instanceof CharSequence) {
                try {
                    args[i] = (int) ((CharSequence) k).charAt(0);
                } catch (IndexOutOfBoundsException ex) {
                    args[i] = 0;
                }
            } else if (k instanceof Number) {
                args[i] = ((Number) k).intValue();
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    @Export("_chr")
    public static Object[] toChar(final Object... args) {
        for (int i = 0; i < args.length; ++i) {
            final Object k = args[i];
            if (k instanceof Character) {
                // Do nothing, it is already a character
            } else if (k instanceof Number) {
                args[i] = (char) ((Number) k).intValue();
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    @Export("_int")
    public static Object[] toInt(final Object... args) {
        for (int i = 0; i < args.length; ++i) {
            final Object k = args[i];
            if (k == null) {
                // Do nothing, null maps to null
            } else if (k instanceof Number) {
                args[i] = ((Number) k).intValue();
            } else if (k instanceof Character) {
                args[i] = (int) ((Character) k).charValue();
            } else {
                try {
                    args[i] = Integer.parseInt(k.toString());
                } catch (NumberFormatException ex) {
                    args[i] = null;
                }
            }
        }
        return args;
    }

    @Export("_float")
    public static Object[] toFloat(final Object... args) {
        for (int i = 0; i < args.length; ++i) {
            final Object k = args[i];
            if (k == null) {
                // Do nothing, null maps to null
            } else if (k instanceof Number) {
                args[i] = ((Number) k).doubleValue();
            } else {
                try {
                    args[i] = Double.parseDouble(k.toString());
                } catch (NumberFormatException ex) {
                    args[i] = null;
                }
            }
        }
        return args;
    }

    @Export("_join")
    public static String join(final Object... args) {
        final StringBuilder sb = new StringBuilder();
        joinHelper(sb, Arrays.asList(args), " ");
        return sb.toString();
    }

    @Export("_djoin")
    public static String joinWithDelim(final Object delim, final Object... args) {
        final StringBuilder sb = new StringBuilder();
        joinHelper(sb, Arrays.asList(args), delim.toString());
        return sb.toString();
    }

    @Export("_concat")
    public static String concat(final Object... args) {
        final StringBuilder sb = new StringBuilder();
        joinHelper(sb, Arrays.asList(args), "");
        return sb.toString();
    }

    @Export("_lines")
    public static String lines(final Object... args) {
        final StringBuilder sb = new StringBuilder();
        joinHelper(sb, Arrays.asList(args), "\n");
        return sb.toString();
    }

    public static boolean joinHelper(final StringBuilder sb, final Collection<?> args, final String c) {
        if (args.isEmpty()) return false;

        boolean flag = false;
        for (final Object k : args) {
            if (flag && c != null) {
                sb.append(c);
            }
            flag = true;

            if (k == null) {
                sb.append("null");
            } else if (k.getClass().isArray()) {
                flag = joinHelper(sb, Arraylib.polyArrayToList(k), c);
            } else if (k instanceof Map) {
                flag = joinHelper(sb, ((Map<?, ?>) k).values(), c);
            } else if (k instanceof Collection) {
                flag = joinHelper(sb, (Collection<?>) k, c);
            } else {
                sb.append(k);
            }
        }
        return true;
    }

    @Export("_p")
    public static Object print(final Object... k) {
        System.out.println(join(k));
        return null;
    }

    @Export("_")
    public static Object ignore(final Object... args) {
        if (args.length == 0) return null;
        return args[args.length - 1];
    }

    @Export("_eqls")
    public static boolean equals(final Object a, final Object b, final Object... c) {
        if (Objects.equals(a, b)) {
            for (final Object k : c) {
                if (!(Objects.equals(a, k) || Objects.equals(b, k))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Export("_truthy")
    public static boolean isTruthy(final Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof Number) return ((Number) obj).doubleValue() != 0;
        if (obj instanceof Character) return ((Character) obj).charValue() != 0;
        return Arraylib.length(obj).longValue() != 0;
    }
}