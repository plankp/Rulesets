/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.IOException;
import java.io.StringReader;

import java.lang.reflect.InvocationTargetException;

import java.util.List;
import java.util.ArrayList;

import com.ymcmp.rset.rt.Rule;
import com.ymcmp.rset.rt.Rulesets;

import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

public class JavaObjTest {

    private static Class<?> JavaObj;

    @BeforeClass
    public static void compile() {
        final StringReader reader = new StringReader(
            "rule field  = ; { (a, b, c)!length + 12!SIZE },\n" +
            "rule method = ; { Hello!(length,) },\n" +
            "rule data   = k:!length { ?k },\n"
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
        assertEquals(new Object[]{ "a", "b", "c" }.length + Integer.SIZE, newJavaObj().getRule("field").apply(new Object[0]));
    }

    @Test
    public void testMethod() {
        assertEquals("Hello".length(), newJavaObj().getRule("method").apply(new Object[0]));
    }

    @Test
    public void testData() {
        final Object[][] tests = {
            { null }, // does not match
            { new Object[0] }, // matches since arrays do have length as a *hidden* field
            { HasLengthAsField.INSTANCE }, // matches since #length is public
            { "Hello, world!" }, // matches since #length() is public
        };

        final List<Object> list = new ArrayList<>();
        final Rule rule = newJavaObj().getRule("data");
        for (final Object[] test : tests) {
            final Object obj = rule.apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertEquals(3, list.size());
        assertSame(tests[1][0], list.get(0));
        assertSame(tests[2][0], list.get(1));
        assertSame(tests[3][0], list.get(2));
    }
}

final class HasLengthAsField {

    public static final HasLengthAsField INSTANCE = new HasLengthAsField();

    public final int length = 10;

    private HasLengthAsField() {
        // Singleton
    }
}