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

public class NegateTest {

    private static Class<?> Negate;

    @BeforeClass
    public static void compile() {
        final StringReader reader = new StringReader(
            "rule a   = k:a { ?k }," +
            "rule na  = k:~a { ?k }," +
            "rule nna = k:~~a { ?k }," +
            "rule r   = k:(5-10) { ?k }," +
            "rule nr  = k:~(5-10) { ?k }," +
            "rule or  = k:(a | b) { ?k }," +
            "rule nor = k:~(a | b) { ?k }," +
            "rule orn = k:(~a | ~b) { ?k }," +
            "rule s   = k:(a b c) { ?k }," +
            "rule ns  = k:~(a b c) { ?k }," +
            "rule range = k:~(1-5) { ?k }," +
            "rule prop  = k:~!hello { ?k },"
        );

        final RsetLexer lexer = new RsetLexer(reader);
        final RsetParser parser = new RsetParser(lexer);
        final byte[] bytes = parser.parse().toBytecode("Negate", null, false);
        final ByteClassLoader bcl = new ByteClassLoader();
        final Class<?> cl = bcl.loadFromBytes("Negate", bytes);
        if (Rulesets.class.isAssignableFrom(cl)) {
            Negate = cl;
        } else {
            throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
        }
    }

    public static Rulesets newNegate() {
        try {
            return (Rulesets) Negate.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testA() {
        final Object[][] tests = {
            { },
            { "a" },
            { "A" },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newNegate();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("a").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "a\n",
                sb.toString());
    }

    @Test
    public void testNA() {
        final Object[][] tests = {
            { },
            { "a" },
            { "A" },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newNegate();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("na").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "A\n",
                sb.toString());
    }

    @Test
    public void testNNA() {
        final Object[][] tests = {
            { },
            { "a" },
            { "A" },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newNegate();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("nna").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "a\n",
                sb.toString());
    }

    @Test
    public void testR() {
        final Object[][] tests = {
            { },
            { 0 },
            { 8 },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newNegate();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("r").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "8\n",
                sb.toString());
    }

    @Test
    public void testNR() {
        final Object[][] tests = {
            { },
            { 0 },
            { 8 },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newNegate();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("nr").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "0\n",
                sb.toString());
    }

    @Test
    public void testOR() {
        final Object[][] tests = {
            { },
            { "a" },
            { "b" },
            { "c" },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newNegate();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("or").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "a\n" +
                "b\n",
                sb.toString());
    }

    @Test
    public void testNOR() {
        final Object[][] tests = {
            { },
            { "a" },
            { "b" },
            { "c" },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newNegate();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("nor").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "c\n",
                sb.toString());
    }

    @Test
    public void testORN() {
        final Object[][] tests = {
            { },
            { "a" },
            { "b" },
            { "c" },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newNegate();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("orn").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "a\n" +
                "b\n" +
                "c\n",
                sb.toString());
    }

    @Test
    public void testS() {
        final Object[][] tests = {
            { },
            { "a" },
            { "a", "b" },
            { "a", "b", "c" },
            { "d" },
            { "d", "e" },
            { "d", "e", "f" },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newNegate();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("s").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "[a, b, c]\n",
                sb.toString());
    }

    @Test
    public void testNS() {
        final Object[][] tests = {
            { },
            { "a" },
            { "a", "b" },
            { "a", "b", "c" },
            { "d" },
            { "d", "e" },
            { "d", "e", "f" },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newNegate();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("ns").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "[d, e, f]\n",
                sb.toString());
    }

    @Test
    public void testRange() {
        final Object[][] tests = {
            { },
            { 3 },
            { "a" },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newNegate();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("range").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "a\n",
                sb.toString());
    }

    @Test
    public void testProp() {
        assertNull(newNegate().getRule("prop").apply(new Object[1]));
    }
}