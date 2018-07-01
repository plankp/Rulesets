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

public class ObjectMatchTest {

    private static Class<?> ObjectMatch;

    @BeforeClass
    public static void compile() {
        final StringReader reader = new StringReader(
            "rule i123 = k:(1 2 3 | (-1) (-2) (-3)) { ?k },\n" +
            "rule f123 = k:(1.0 2.0 3.0 | (-1.0) (-2.0) (-3.0)) { ?k },\n" +
            "rule group = k:(1 2 3, 4 5 6) { ?k },\n" +
            "rule chars = k:%abc { ?k },\n" +
            "rule destr = k:[1 2 3] { ?k },\n" +
            "rule null  = () { 'Found null!' },\n"
        );

        final RsetLexer lexer = new RsetLexer(reader);
        final RsetParser parser = new RsetParser(lexer);
        final byte[] bytes = parser.parse().toBytecode("ObjectMatch", null, true);
        final ByteClassLoader bcl = new ByteClassLoader();
        final Class<?> cl = bcl.loadFromBytes("ObjectMatch", bytes);
        if (Rulesets.class.isAssignableFrom(cl)) {
            ObjectMatch = cl;
        } else {
            throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
        }
    }

    public static Rulesets newObjectMatch() {
        try {
            return (Rulesets) ObjectMatch.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testI123() {
        final Object[][] tests = {
            { 1, 2, 3 },
            { 1, -2, 3 },
            { -1, -2, -3 },
            { 1.0, 2.0, 3.0 },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newObjectMatch();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("i123").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "[1, 2, 3]\n" +
                "[-1, -2, -3]\n",
                sb.toString());
    }

    @Test
    public void testF123() {
        final Object[][] tests = {
            { 1, 2, 3 },
            { 1.0, 2.0, 3.0 },
            { 1.0, -2.0, 3.0 },
            { -1.0, -2.0, -3.0 },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newObjectMatch();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("f123").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "[1.0, 2.0, 3.0]\n" +
                "[-1.0, -2.0, -3.0]\n",
                sb.toString());
    }

    @Test
    public void testGroup() {
        final Object[][] tests = {
            { 1, 2, 3 },
            { 4, 5, 6 },
            { 1, 2, 3, 4, 5, 6 },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newObjectMatch();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("group").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "[[1, 2, 3], [4, 5, 6]]\n",
                sb.toString());
    }

    @Test
    public void testChars() {
        final Object[][] tests = {
            { 'a', 'b' },
            { 'a', 'c' },
            { 'a', 'b', 'c' },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newObjectMatch();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("chars").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "[a, b, c]\n",
                sb.toString());
    }

    @Test
    public void testDestr() {
        final Object[][] tests = {
            { },
            { 1, 2, 3 },
            { Arrays.asList(1, 2) },
            { new Object[]{ 1, 2, 3 } },
            { Arrays.asList(1, 2, 3) },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newObjectMatch();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("destr").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "[1, 2, 3]\n" +
                "[1, 2, 3]\n",
                sb.toString());
    }

    @Test
    public void testNull() {
        final Object[][] tests = {
            { }, // empty is not null
            { null },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newObjectMatch();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("null").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals("Found null!\n", sb.toString());
    }
}