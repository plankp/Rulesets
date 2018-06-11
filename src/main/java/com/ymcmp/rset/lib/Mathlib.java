package com.ymcmp.rset.lib;

public final class Mathlib {

    private Mathlib() {
        //
    }

    @Export("_add")
    @Varargs(2)
    public static double add(double a, double b, Object... c) {
        double k = a + b;
        for (final Object i : c) {
            k += ((Number) i).doubleValue();
        }
        return k;
    }

    @Export("_sub")
    @Varargs(2)
    public static double sub(double a, double b, Object... c) {
        double k = a - b;
        for (final Object i : c) {
            k -= ((Number) i).doubleValue();
        }
        return k;
    }

    @Export("_mul")
    @Varargs(2)
    public static double mul(double a, double b, Object... c) {
        double k = a * b;
        for (final Object i : c) {
            k *= ((Number) i).doubleValue();
        }
        return k;
    }

    @Export("_div")
    @Varargs(2)
    public static double div(double a, double b, Object... c) {
        double k = a / b;
        for (final Object i : c) {
            k /= ((Number) i).doubleValue();
        }
        return k;
    }

    @Export("_mod")
    @Varargs(2)
    public static double mod(double a, double b, Object... c) {
        double k = a % b;
        for (final Object i : c) {
            k %= ((Number) i).doubleValue();
        }
        return k;
    }

    @Export("_pow")
    public static double pow(double a, double b) {
        return Math.pow(a, b);
    }

    @Export("_sqrt")    // make it varargs
    public static double sqrt(double a) {
        return Math.sqrt(a);
    }

    @Export
    @Varargs(2)
    public static int _and(int a, int b, Object[] c) {
        int k = a & b;
        for (final Object i : c) {
            k &= ((Number) i).intValue();
        }
        return k;
    }

    @Export
    @Varargs(2)
    public static int _or(int a, int b, Object[] c) {
        int k = a | b;
        for (final Object i : c) {
            k |= ((Number) i).intValue();
        }
        return k;
    }

    @Export
    @Varargs(2)
    public static int _xor(int a, int b, Object[] c) {
        int k = a ^ b;
        for (final Object i : c) {
            k ^= ((Number) i).intValue();
        }
        return k;
    }

    @Export
    public static Object _not(Object k) {
        if (k instanceof Number) return ~((Number) k).intValue();
        if (k instanceof Boolean) return !((Boolean) k);
        return null;
    }

    @Export
    public static boolean _lt(Comparable a, Comparable b) {
        return a.compareTo(b) < 0;
    }

    @Export
    public static boolean _gt(Comparable a, Comparable b) {
        return a.compareTo(b) > 0;
    }

    @Export
    public static boolean _le(Comparable a, Comparable b) {
        return a.compareTo(b) <= 0;
    }

    @Export
    public static boolean _ge(Comparable a, Comparable b) {
        return a.compareTo(b) >= 0;
    }
}