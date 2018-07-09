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

public class LexerTest {

    private static Class<?> Lexer;

    @BeforeClass
    public static void compile() throws IOException {
        final InputStreamReader reader = new InputStreamReader(LexerTest.class.getResourceAsStream("/Lexer.rules"));

        try (final RsetLexer lexer = new RsetLexer(reader)) {
            final RsetParser parser = new RsetParser(lexer);
            final byte[] bytes = parser.parse().toBytecode("Lexer", null, false);
            final ByteClassLoader bcl = new ByteClassLoader();
            final Class<?> cl = bcl.loadFromBytes("Lexer", bytes);
            if (Rulesets.class.isAssignableFrom(cl)) {
                Lexer = cl;
            } else {
                throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
            }
        }
    }

    public static Rulesets newLexer() {
        try {
            return (Rulesets) Lexer.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Character[] strToCharArray(String str) {
        if (str == null || str.isEmpty()) return new Character[0];
        return str.chars().mapToObj(e -> (char) e).toArray(Character[]::new);
    }

    @Test
    public void testStrS() {
        final Object[][] tests = {
            strToCharArray("''"),
            strToCharArray("'Hello, world'"),
            strToCharArray("'\\a\\b\\r\"\\''"),
            strToCharArray("'\\k'"),    // Illegal escape
            strToCharArray("'Foobar"),  // Unterminated literal
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newLexer();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("str_s").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "str", "''" },
            { "str", "'Hello, world'" },
            { "str", "'\\a\\b\\r\"\\''" },
        }, list.toArray());
    }

    @Test
    public void testStrD() {
        final Object[][] tests = {
            strToCharArray("\"\""),
            strToCharArray("\"Hello, world\""),
            strToCharArray("\"\\a\\b\\r'\\\"\""),
            strToCharArray("\"\\k'"),    // Illegal escape
            strToCharArray("\"Foobar"),  // Unterminated literal
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newLexer();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("str_d").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "str", "\"\"" },
            { "str", "\"Hello, world\"" },
            { "str", "\"\\a\\b\\r'\\\"\"" },
        }, list.toArray());
    }

    @Test
    public void testNum() {
        final Object[][] tests = {
            strToCharArray("0"),
            strToCharArray("123"),
            strToCharArray("1.23"),
            strToCharArray(".5"),
            strToCharArray("."),
            strToCharArray("1."),
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newLexer();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("num").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "int", "0" },
            { "int", "123" },
            { "real", "1.23" },
            { "real", ".5" },
            { "real", "." },
            { "real", "1." },
        }, list.toArray());
    }

    @Test
    public void testIdent() {
        final Object[][] tests = {
            strToCharArray("a"),
            strToCharArray("1"),
            strToCharArray("$"),
            strToCharArray("_"),
            strToCharArray("$a_very_perl_like_VARNAME1"),
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newLexer();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("ident").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "ident", "a" },
            { "ident", "1" },
            { "ident", "$" },
            { "ident", "_" },
            { "ident", "$a_very_perl_like_VARNAME1" },
        }, list.toArray());
    }

    @Test
    public void testGetToken() {
        final Object[][] tests = {
            strToCharArray("a"),
            strToCharArray("$"),
            strToCharArray("_"),
            strToCharArray("$a_very_perl_like_VARNAME1"),

            strToCharArray("0"),
            strToCharArray("123"),
            strToCharArray("1.23"),
            strToCharArray(".5"),
            strToCharArray("."),
            strToCharArray("1."),

            strToCharArray("\"\""),
            strToCharArray("\"Hello, world\""),
            strToCharArray("\"\\a\\b\\r'\\\"\""),

            strToCharArray("''"),
            strToCharArray("'Hello, world'"),
            strToCharArray("'\\a\\b\\r\"\\''"),

            strToCharArray(","),
            strToCharArray("="),
            strToCharArray(";"),
            strToCharArray("# I am a comment\n  ?"),
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newLexer();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("getToken").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "ident", "a" },
            { "ident", "$" },
            { "ident", "_" },
            { "ident", "$a_very_perl_like_VARNAME1" },

            { "int", "0" },
            { "int", "123" },
            { "real", "1.23" },
            { "real", ".5" },
            { "real", "." },
            { "real", "1." },

            { "str", "\"\"" },
            { "str", "\"Hello, world\"" },
            { "str", "\"\\a\\b\\r'\\\"\"" },

            { "str", "''" },
            { "str", "'Hello, world'" },
            { "str", "'\\a\\b\\r\"\\''" },

            { "CM", "," },
            { "EQ", "=" },
            { "SM", ";" },
            { "QM", "?" },
        }, list.toArray());
    }
}