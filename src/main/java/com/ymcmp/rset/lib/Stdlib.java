package com.ymcmp.rset.lib;

import java.util.Map;
import java.util.Objects;
import java.util.Collection;
import java.util.function.Function;

public final class Stdlib {

    @Export public static final char _space = ' ';
    @Export public static final char _tab = '\t';
    @Export public static final char _bs = '\b';
    @Export public static final char _lf = '\n';
    @Export public static final char _cr = '\r';
    @Export public static final Object _null = null;

    private Stdlib() {
        // Do nothing
    }

    @Export
    @Varargs
    public static Object[] _ord(final Object[] args) {
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

    @Export
    @Varargs
    public static Object[] _chr(final Object[] args) {
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

    @Export
    @Varargs
    public static Object[] _int(final Object[] args) {
        for (int i = 0; i < args.length; ++i) {
            final Object k = args[i];
            if (k instanceof Number) {
                args[i] = ((Number) k).intValue();
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

    @Export
    @Varargs
    public static Object[] _float(final Object[] args) {
        for (int i = 0; i < args.length; ++i) {
            final Object k = args[i];
            if (k instanceof Number) {
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

    @Export
    @Varargs
    public static String _join(final Object[] args) {
        final StringBuilder sb = new StringBuilder();
        joinHelper(sb, args, " ");
        return sb.toString();
    }

    @Export
    @Varargs(1)
    public static String _djoin(final Object delim, final Object[] args) {
        final StringBuilder sb = new StringBuilder();
        joinHelper(sb, args, delim.toString());
        return sb.toString();
    }

    @Export("_concat")
    @Varargs
    public static String concat(final Object... args) {
        final StringBuilder sb = new StringBuilder();
        joinHelper(sb, args, "");
        return sb.toString();
    }

    @Export
    @Varargs
    public static String _lines(final Object[] args) {
        final StringBuilder sb = new StringBuilder();
        joinHelper(sb, args, "\n");
        return sb.toString();
    }

    public static void joinHelper(final StringBuilder sb, final Object[] args, final String c) {
        for (int i = 0; i < args.length; ++i) {
            if (i != 0 && c != null) sb.append(c);
            final Object k = args[i];
            if (k == null) {
                sb.append("null");
            } else if (k.getClass().isArray()) {
                joinHelper(sb, (Object[]) k, c);
            } else if (k instanceof Map) {
                joinHelper(sb, ((Map<?, ?>) k).values().toArray(), c);
            } else if (k instanceof Collection) {
                joinHelper(sb, ((Collection<?>) k).toArray(), c);
            } else {
                sb.append(k);
            }
        }
    }

    @Export("_")
    @Varargs
    public static Object ignore(final Object... args) {
        if (args.length == 0) return null;
        return args[args.length - 1];
    }

    @Export("_subs")
    public static Object subscript(final Object base, final Object offset) {
        if (base == null) return null;
        try {
            if (base.getClass().isArray()) {
                return ((Object[]) base)[(Integer) offset];
            }
            if (base instanceof CharSequence) {
                return ((CharSequence) base).charAt((Integer) offset);
            }
            if (base instanceof Collection) {
                return ((Collection<?>) base).toArray()[(Integer) offset];
            }
            if (base instanceof Map) {
                return ((Map<?, ?>) base).get(offset);
            }

            throw new RuntimeException("Type '" + base.getClass().getSimpleName() + "' cannot be indexed!");
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    @Export("_eqls")
    @Varargs(2)
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
        if (obj instanceof Collection) return !((Collection<?>) obj).isEmpty();
        if (obj instanceof CharSequence) return ((CharSequence) obj).length() != 0;
        if (obj instanceof Map) return !((Map<?, ?>) obj).isEmpty();
        if (obj instanceof Function) return true;
        if (obj.getClass().isArray()) return ((Object[]) obj).length != 0;
        return true;
    }
}