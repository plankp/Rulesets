/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.IOException;
import java.io.StringReader;

import java.lang.reflect.InvocationTargetException;

import java.util.Map;

import com.ymcmp.rset.rt.Rulesets;
import com.ymcmp.rset.lib.Extensions;

import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

public class JavaObjTest {

    private static Class<?> JavaObj;

    @BeforeClass
    public static void compile() {
        final StringReader reader = new StringReader(
            "rule field  = ; { (a, b, c)!length + 12!SIZE },\n" +
            "rule method = ; { Hello!(length,) },\n"
        );

        final RsetLexer lexer = new RsetLexer(reader);
        final RsetParser parser = new RsetParser(lexer);
        final byte[] bytes = parser.parse().toBytecode("JavaObj", null, true);
        final ByteClassLoader bcl = new ByteClassLoader();
        final Class<?> cl = bcl.loadFromBytes("JavaObj", bytes);
        if (Rulesets.class.isAssignableFrom(cl)) {
            JavaObj = cl;
        } else {
            throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
        }
    }

    public static Rulesets newJavaObj() {
        try {
            return (Rulesets) JavaObj.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testField() {
        assertEquals(new Object[]{ "a", "b", "c" }.length + Integer.SIZE * 1.0, newJavaObj().getRule("field").apply(new Object[0]));
    }

    @Test
    public void testMethod() {
        assertEquals("Hello".length(), newJavaObj().getRule("method").apply(new Object[0]));
    }
}