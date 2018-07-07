/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.IOException;
import java.io.StringReader;

import java.lang.reflect.InvocationTargetException;

import com.ymcmp.rset.rt.Rulesets;

import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

public class MacroTest {

    private static Class<?> Macro;

    private Rulesets rsets;

    @BeforeClass
    public static void compile() {
        final StringReader reader = new StringReader(
            "fragment a = &0,\n" +
            "rule expectA = k:&a/a { ?k },\n" +
            "rule expectB = k:&a/b { ?k },\n"
        );

        final RsetLexer lexer = new RsetLexer(reader);
        final RsetParser parser = new RsetParser(lexer);
        final byte[] bytes = parser.parse().toBytecode("Macro", null, false);
        final ByteClassLoader bcl = new ByteClassLoader();
        final Class<?> cl = bcl.loadFromBytes("Macro", bytes);
        if (Rulesets.class.isAssignableFrom(cl)) {
            Macro = cl;
        } else {
            throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
        }
    }

    @Before
    public void initRsets() {
        try {
            rsets = (Rulesets) Macro.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testExpectA() {
        final Object[][] tests = {
            { "a" },
            { "b" },
        };

        final StringBuilder sb = new StringBuilder();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("expectA").apply(test);
            if (obj != null) sb.append(obj);
        }
        assertEquals("a", sb.toString());
    }

    @Test
    public void testExpectB() {
        final Object[][] tests = {
            { "a" },
            { "b" },
        };

        final StringBuilder sb = new StringBuilder();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("expectB").apply(test);
            if (obj != null) sb.append(obj);
        }
        assertEquals("b", sb.toString());
    }
}