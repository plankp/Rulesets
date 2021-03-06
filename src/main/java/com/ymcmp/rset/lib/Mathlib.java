/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.lib;

import java.math.BigDecimal;

import java.util.function.IntBinaryOperator;
import java.util.function.DoubleBinaryOperator;

public final class Mathlib {

    private Mathlib() {
        //
    }

    private static double doubleReduction(double a, double b, Object[] c, DoubleBinaryOperator op) {
        double k = op.applyAsDouble(a, b);
        for (final Object i : c) {
            k = op.applyAsDouble(k, ((Number) i).doubleValue());
        }
        return k;
    }

    private static int intReduction(int a, int b, Object[] c, IntBinaryOperator op) {
        int k = op.applyAsInt(a, b);
        for (final Object i : c) {
            k = op.applyAsInt(k, ((Number) i).intValue());
        }
        return k;
    }

    @Export("_add")
    public static double add(double a, double b, Object... c) {
        return doubleReduction(a, b, c, (lhs, rhs) -> lhs + rhs);
    }

    @Export("_sub")
    public static double sub(double a, double b, Object... c) {
        return doubleReduction(a, b, c, (lhs, rhs) -> lhs - rhs);
    }

    @Export("_mul")
    public static double mul(double a, double b, Object... c) {
        return doubleReduction(a, b, c, (lhs, rhs) -> lhs * rhs);
    }

    @Export("_div")
    public static double div(double a, double b, Object... c) {
        return doubleReduction(a, b, c, (lhs, rhs) -> lhs / rhs);
    }

    @Export("_mod")
    public static double mod(double a, double b, Object... c) {
        return doubleReduction(a, b, c, (lhs, rhs) -> lhs % rhs);
    }

    @Export("_pow")
    public static double pow(double a, double b) {
        return Math.pow(a, b);
    }

    @Export("_sqrt")    // make it varargs
    public static double sqrt(double a) {
        return Math.sqrt(a);
    }

    @Export("_and")
    public static int and(int a, int b, Object... c) {
        return intReduction(a, b, c, (lhs, rhs) -> lhs & rhs);
    }

    @Export("_or")
    public static int or(int a, int b, Object... c) {
        return intReduction(a, b, c, (lhs, rhs) -> lhs | rhs);
    }

    @Export("_xor")
    public static int xor(int a, int b, Object... c) {
        return intReduction(a, b, c, (lhs, rhs) -> lhs ^ rhs);
    }

    @Export("_not")
    public static Object not(Object k) {
        if (k instanceof Number) return ~((Number) k).intValue();
        if (k instanceof Boolean) return !((Boolean) k);
        return null;
    }

    @Export
    public static boolean _lt(Comparable a, Comparable b) {
        return compare(a, b) < 0;
    }

    @Export
    public static boolean _gt(Comparable a, Comparable b) {
        return compare(a, b) > 0;
    }

    @Export
    public static boolean _le(Comparable a, Comparable b) {
        return compare(a, b) <= 0;
    }

    @Export
    public static boolean _ge(Comparable a, Comparable b) {
        return compare(a, b) >= 0;
    }

    @Export("_cmp")
    public static int compare(Comparable a, Comparable b) {
        // Comparing 0:Number and 1.0:Number will cause a
        // ClassCastException which is not what we want
        if (a instanceof Number && b instanceof Number) {
            final BigDecimal lhs = new BigDecimal(a.toString());
            final BigDecimal rhs = new BigDecimal(b.toString());
            return lhs.compareTo(rhs);
        }
        return a.compareTo(b);
    }
}