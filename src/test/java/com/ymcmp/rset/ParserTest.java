/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.IOException;
import java.io.InputStreamReader;

import java.lang.reflect.InvocationTargetException;

import java.util.List;
import java.util.ArrayList;

import com.ymcmp.rset.rt.Rulesets;

import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

public class ParserTest {

    private static Class<?> Parser;

    @BeforeClass
    public static void compile() throws IOException {
        final InputStreamReader reader = new InputStreamReader(LexerTest.class.getResourceAsStream("/Parser.rules"));

        try (final RsetLexer lexer = new RsetLexer(reader)) {
            final RsetParser parser = new RsetParser(lexer);
            final byte[] bytes = parser.parse().toBytecode("Parser", null, false);
            final ByteClassLoader bcl = new ByteClassLoader();
            final Class<?> cl = bcl.loadFromBytes("Parser", bytes);
            if (Rulesets.class.isAssignableFrom(cl)) {
                Parser = cl;
            } else {
                throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
            }
        }
    }

    public static Rulesets newLexer() {
        try {
            return (Rulesets) Parser.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testParseValue() {
        final Object[][] tests = {
            { new Object[] { "MN", "-" }, new Object[] { "int", "10" } },
            { new Object[] { "MD", "%" }, new Object[] { "str", "'Foo'" } },
            { new Object[] { "ident", "is_value" } },
            { new Object[] { "MN", "-" }, new Object[] { "ident", "a" } }, // Illegal parse
            { new Object[] { "MD", "%" }, new Object[] { "MN", "-" } }, // Illegal parse
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newLexer();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("parseValue").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "int", "-10" },
            { "chars", "%'Foo'" },
            { "ident", "is_value" },
        }, list.toArray());
    }

    @Test
    public void testParseAtomic() {
        final Object[][] tests = {
            { new Object[] { "ST", "*" } },
            { new Object[] { "SM", ";" } },
            { new Object[] { "TD", "~" }, new Object[] { "int", "5" } },
            { new Object[] { "AM", "&" }, new Object[] { "ident", "a" } },
            { new Object[] { "AM", "&" }, new Object[] { "ident", "b" }, new Object[] { "DV", "/" }, new Object[] { "ST", "*" }, new Object[] { "DV", "/" }, new Object[] { "SM", ";" } },
            { new Object[] { "EX", "!" }, new Object[] { "ident", "s" } },
            { new Object[] { "LA", "<" }, new Object[] { "ident", "t" } },
            { new Object[] { "RA", ">" }, new Object[] { "ident", "u" } },
            { new Object[] { "LS", "[" }, new Object[] { "RS", "]" } },
            { new Object[] { "LS", "[" }, new Object[] { "int", "5" }, new Object[] { "RS", "]" } },
            { new Object[] { "LP", "(" }, new Object[] { "RP", ")" } },
            { new Object[] { "LP", "(" }, new Object[] { "real", "1.2" }, new Object[] { "RP", ")" } },
            { new Object[] { "LP", "(" }, new Object[] { "MD", "%" }, new Object[] { "ident", "foo" }, new Object[] { "CM", "," }, new Object[] { "RP", ")" } },
            { new Object[] { "LP", "(" }, new Object[] { "ident", "a" }, new Object[] { "CM", "," }, new Object[] { "ident", "b" }, new Object[] { "RP", ")" } },
            { new Object[] { "ident", "a" }, new Object[] { "CO", ":" }, new Object[] { "int", "5" } },
            { new Object[] { "int", "0" }, new Object[] { "MN", "-" }, new Object[] { "int", "5" } },
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newLexer();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("parseAtomic").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "slot_occupied" },
            { "end_of_data" },
            { "negate", new Object[] { "int", "5" } },
            { "ref", new Object[] { "ident", "a" }, new Object[0] },
            { "ref", new Object[] { "ident", "b" }, new Object[] { new Object[] { "slot_occupied" }, new Object[] { "end_of_data" } } },
            { "resp", new Object[] { "ident", "s" } },
            { "base_of", new Object[] { "ident", "t" } },
            { "subclass_of", new Object[] { "ident", "u" } },
            { "destr", new Object[] { "end_of_data" } },
            { "destr", new Object[] { "int", "5" } },
            { "null" },
            { "real", "1.2" },
            { "group", new Object[] { new Object[] { "chars", "%foo" } } },
            { "group", new Object[] { new Object[] { "ident", "a" }, new Object[] { "ident", "b" } } },
            { "capture", new Object[] { "ident", "a" }, new Object[] { "int", "5" } },
            { "range", new Object[] { "int", "0" }, new Object[] { "int", "5" } },
        }, list.toArray());
    }

    @Test
    public void testParseInnerLoop() {
        final Object[][] tests = {
            { new Object[] { "int", "1" } },
            { new Object[] { "int", "10" }, new Object[] { "ST", "*" } },
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newLexer();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("parseInnerLoop").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "int", "1" },
            { "*", new Object[] { "int", "10" } },
        }, list.toArray());
    }

    @Test
    public void testParseRule() {
        final Object[][] tests = {
            { new Object[] { "int", "1" }, new Object[] { "int", "2" } },
            { new Object[] { "int", "1" }, new Object[] { "OR", "|" }, new Object[] { "int", "2" } },
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newLexer();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("parseRule").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "seq", new Object[] { new Object[] { "int", "1" }, new Object[] { "int", "2" } } },
            { "|", new Object[] { new Object[] { "int", "1" } , new Object[] { "int", "2" } } },
        }, list.toArray());
    }
}