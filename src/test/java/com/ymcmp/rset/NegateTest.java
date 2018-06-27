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
            "rule nna = k:~~a { ?k },"
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
}