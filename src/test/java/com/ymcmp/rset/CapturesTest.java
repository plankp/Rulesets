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
import com.ymcmp.rset.lib.Extensions;

import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

public class CapturesTest {

    private static Class<?> Captures;
    private static Extensions ext;

    @BeforeClass
    public static void compile() {
        // This Ruleset does not have any standard exports! (see line 50, feature mask = 0)
        final StringReader reader = new StringReader(
            "rule xs = (xs:(x+)) u* { ?xs },\n" +
            "rule ys = ys:(y*) { ?ys },\n" +
            "rule xy = xy:((&xs | y+)+) { ?xy },\n" +
            "rule ms = ms:((a b c)?) { ?ms },\n"
        );

        final RsetLexer lexer = new RsetLexer(reader);
        final RsetParser parser = new RsetParser(lexer);
        final byte[] bytes = parser.parse().toBytecode("Captures", null, false);
        final ByteClassLoader bcl = new ByteClassLoader();
        final Class<?> cl = bcl.loadFromBytes("Captures", bytes);
        if (Rulesets.class.isAssignableFrom(cl)) {
            Captures = cl;
        } else {
            throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
        }

        ext = new Extensions(0);
    }

    public static Rulesets newCaptures() {
        try {
            return (Rulesets) Captures.getConstructor(Extensions.class).newInstance(ext);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void featureMaskOnExtIsZero() {
        assertEquals(0, ext.getEnabledFeatures());
    }

    @Test
    public void testGetRuleNames() {
        final Set<String> rsets = newCaptures().getRuleNames();
        assertEquals(4, rsets.size());
        assertTrue(rsets.contains("xs"));
        assertTrue(rsets.contains("ys"));
        assertTrue(rsets.contains("xy"));
        assertTrue(rsets.contains("ms"));
    }

    @Test
    public void testXS() {
        final Object[][] tests = {
            { },
            { "x" },
            { "x", "x" },
            { "x", "x", "x" },  // This should be enough...
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newCaptures();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("xs").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "[x]\n" +
                "[x, x]\n" +
                "[x, x, x]\n",
                sb.toString());
    }

    @Test
    public void testYS() {
        final Object[][] tests = {
            { },
            { "y" },
            { "y", "y" },
            { "y", "y", "y" },  // This should be enough...
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newCaptures();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("ys").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "[]\n" +
                "[y]\n" +
                "[y, y]\n" +
                "[y, y, y]\n",
                sb.toString());
    }

    @Test
    public void testXY() {
        final Object[][] tests = {
            { },
            { "y" },
            { "y", "x" },
            { "x", "y", "y" },
            { "x", "y", "x", "y" },
            { "y", "y", "y", "y" },
            { "x", "x", "x", "x" },
            { "y", "x", "x", "y" },
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newCaptures();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("xy").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "[[y]]\n" +
                "[[y], [x]]\n" +
                "[[x], [y, y]]\n" +
                "[[x], [y], [x], [y]]\n" +
                "[[y, y, y, y]]\n" +
                "[[x, x, x, x]]\n" +
                "[[y], [x, x], [y]]\n",
                sb.toString());
    }

    @Test
    public void testMS() {
        final Object[][] tests = {
            { },
            { "a" },
            { "a", "b" },
            { "a", "b", "c" },  // This should be enough...
        };

        final StringBuilder sb = new StringBuilder();
        final Rulesets rsets = newCaptures();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("ms").apply(test);
            if (obj != null) {
                sb.append(obj.getClass().isArray() ? Arrays.toString((Object[]) obj) : obj).append('\n');
            }
        }
        assertEquals(
                "[]\n" +
                "[]\n" +
                "[]\n" +
                "[a, b, c]\n",
                sb.toString());
    }
}