/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.lib;

public final class Mathlib {

    private Mathlib() {
        //
    }

    @Export("_add")
    public static double add(double a, double b, Object... c) {
        double k = a + b;
        for (final Object i : c) {
            k += ((Number) i).doubleValue();
        }
        return k;
    }

    @Export("_sub")
    public static double sub(double a, double b, Object... c) {
        double k = a - b;
        for (final Object i : c) {
            k -= ((Number) i).doubleValue();
        }
        return k;
    }

    @Export("_mul")
    public static double mul(double a, double b, Object... c) {
        double k = a * b;
        for (final Object i : c) {
            k *= ((Number) i).doubleValue();
        }
        return k;
    }

    @Export("_div")
    public static double div(double a, double b, Object... c) {
        double k = a / b;
        for (final Object i : c) {
            k /= ((Number) i).doubleValue();
        }
        return k;
    }

    @Export("_mod")
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
    public static int _and(int a, int b, Object... c) {
        int k = a & b;
        for (final Object i : c) {
            k &= ((Number) i).intValue();
        }
        return k;
    }

    @Export
    public static int _or(int a, int b, Object... c) {
        int k = a | b;
        for (final Object i : c) {
            k |= ((Number) i).intValue();
        }
        return k;
    }

    @Export
    public static int _xor(int a, int b, Object... c) {
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