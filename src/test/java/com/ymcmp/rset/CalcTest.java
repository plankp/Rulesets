/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.IOException;
import java.io.StringReader;

import java.lang.reflect.InvocationTargetException;

import java.util.Map;
import java.util.Set;
import java.util.Arrays;

import com.ymcmp.rset.rt.Rulesets;

import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

public class CalcTest {

    private static Class<?> Calc;

    @BeforeClass
    public static void compile() {
        final StringReader reader = new StringReader(
            "fragment ws = (%' '|%'\t')*," +
            "rule number = a:(%'+'|%'-')? &ws b:(%0|%1-%9(%0-%9)*) { (?_float?a~?b):0 }," +
            "rule basic = n:&number | %'(' &ws e:&expr &ws %')' { ?e|?n }," +
            "rule mul = head:&basic tail:((&ws (%'*'|%'/') &ws &basic)*) {" +
            "    ret = ?head;" +
            "    ?tail {" +
            "        p = ?_it:1;" +
            "        t = ?_it:3;" +
            "        ret = ((?_eqls '*' ?p~) & ?ret * ?t, | ?ret / ?t,):0" +
            "    };" +
            "    ?ret" +
            "}," +
            "rule add = head:&mul tail:((&ws(%'+'|%'-')&ws&mul)*) {" +
            "    ret=?head;?tail{t=?_it:3;ret=((?_eqls'+'?_it:1~)&?ret+?t,|?ret-?t,):0};?ret" +
            "}," +
            "rule expr = k:&add { ?k },"
        );

        final RsetLexer lexer = new RsetLexer(reader);
        final RsetParser parser = new RsetParser(lexer);
        final byte[] bytes = parser.parse().toBytecode("Calc", null, false);
        final ByteClassLoader bcl = new ByteClassLoader();
        final Class<?> cl = bcl.loadFromBytes("Calc", bytes);
        if (Rulesets.class.isAssignableFrom(cl)) {
            Calc = cl;
        } else {
            throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
        }
    }

    public static Rulesets newCalc() {
        try {
            return (Rulesets) Calc.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Character[] strToCharArray(String str) {
        if (str == null || str.isEmpty()) return new Character[0];
        return str.chars().mapToObj(e -> (char) e).toArray(Character[]::new);
    }

    @Test
    public void testExpr() {
        final Object[][] tests = {
            strToCharArray("2 * 3 + 4"),
            strToCharArray("1 + 2 * 3 - 0"),
            strToCharArray("1 + -2 + 3 + -4"),
            strToCharArray("1 * 2 * 3 * 4 / 5 * 5 "),
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newCalc();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("expr").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "10.0\n" +
                "7.0\n" +
                "-2.0\n" +
                "24.0\n",
                sb.toString());
    }

    @Test
    public void testNumber() {
        final Object[][] tests = {
            strToCharArray(""),
            strToCharArray("0"),
            strToCharArray("+3"),
            strToCharArray("120"),
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newCalc();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("number").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "0.0\n" +
                "3.0\n" +
                "120.0\n",
                sb.toString());
    }

    @Test
    public void testMul() {
        final Object[][] tests = {
            strToCharArray(""),
            strToCharArray("0"),
            strToCharArray("1*3"),
            strToCharArray("1 * 0"),
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newCalc();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("mul").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "0.0\n" +
                "3.0\n" +
                "0.0\n",
                sb.toString());
    }

    @Test
    public void testAdd() {
        final Object[][] tests = {
            strToCharArray(""),
            strToCharArray("0"),
            strToCharArray("1-3"),
            strToCharArray("1 + 2/2"),
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newCalc();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("add").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "0.0\n" +
                "-2.0\n" +
                "2.0\n",
                sb.toString());
    }

    @Test
    public void testBasic() {
        final Object[][] tests = {
            strToCharArray(""),
            strToCharArray("0"),
            strToCharArray("(1)"),
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newCalc();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("basic").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "0.0\n" +
                "1.0\n",
                sb.toString());
    }
}