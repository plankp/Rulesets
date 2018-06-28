/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.IOException;
import java.io.InputStreamReader;

import java.lang.reflect.InvocationTargetException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;

import com.ymcmp.rset.rt.Rulesets;

import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

public class ExfTest {

    private static Class<?> Exf;

    @BeforeClass
    public static void compile() throws IOException {
        final InputStreamReader reader = new InputStreamReader(ExfTest.class.getResourceAsStream("/exf.rules"));

        try (final RsetLexer lexer = new RsetLexer(reader)) {
            final RsetParser parser = new RsetParser(lexer);
            final byte[] bytes = parser.parse().toBytecode("Exf", null, false);
            final ByteClassLoader bcl = new ByteClassLoader();
            final Class<?> cl = bcl.loadFromBytes("Exf", bytes);
            if (Rulesets.class.isAssignableFrom(cl)) {
                Exf = cl;
            } else {
                throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
            }
        }
    }

    public static Rulesets newExf() {
        try {
            return (Rulesets) Exf.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Character[] strToCharArray(String str) {
        if (str == null || str.isEmpty()) return new Character[0];
        return str.chars().mapToObj(e -> (char) e).toArray(Character[]::new);
    }

    @Test
    public void testScom() {
        final Object[][] tests = {
            strToCharArray("%%! Hello, please ignore me!"),
            strToCharArray("%%! Me too!\n"),
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newExf();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("scom").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "scom", " Hello, please ignore me!" },
            { "scom", " Me too!\n" },
        }, list.toArray());
    }

    @Test
    public void testMcom() {
        final Object[][] tests = {
            strToCharArray("%%+%%-"),
            strToCharArray("%%+ Me too!\n What else?\n %%-"),
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newExf();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("mcom").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "mcom", "" },
            { "mcom", " Me too!\n What else?\n" },
        }, list.toArray());
    }

    @Test
    public void testDefn() {
        final Object[][] tests = {
            strToCharArray("%%defn EMPTY"),
            strToCharArray("%%defn AUTHOR Foo Bar"),
            strToCharArray("%%defn EMAIL foo.bar@null.com"),
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newExf();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("defn").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "defn", "%%EMPTY", "" },
            { "defn", "%%AUTHOR", "Foo Bar" },
            { "defn", "%%EMAIL", "foo.bar@null.com" },
        }, list.toArray());
    }

    @Test
    public void testPrnt() {
        final Object[][] tests = {
            strToCharArray("%%prnt "),
            strToCharArray("%%prnt Just a standard message"),
            strToCharArray("%%prnt Don't %%you dare subst me!"),
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newExf();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("prnt").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "prnt", "" },
            { "prnt", "Just a standard message" },
            { "prnt", "Don't %%you dare subst me!" },
        }, list.toArray());
    }

    @Test
    public void testSbst() {
        final Object[][] tests = {
            strToCharArray("%%sbst "),
            strToCharArray("%%sbst AUTHOR"),
            strToCharArray("%%sbst Crazy variable names... why?"),
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newExf();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("sbst").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "sbst", "%%" },
            { "sbst", "%%AUTHOR" },
            { "sbst", "%%Crazy variable names... why?" },
        }, list.toArray());
    }

    @Test
    public void testMessage() {
        final Object[][] tests = {
            strToCharArray("Hello, world!"),
            strToCharArray("Today is %%DATE"),
            strToCharArray("%%DOCNAME written by %%AUTHOR has been published"),
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newExf();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("message").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        assertArrayEquals(new Object[][] {
            { "seq", new Object[]{ new Object[]{ "prnt", "" }, new Object[]{ "prnt", "Hello," }, new Object[]{ "prnt", " " }, new Object[]{ "prnt", "world!" }, new Object[]{ "prnt", "" }, new Object[]{ "prnt", "" } } },
            { "seq", new Object[]{ new Object[]{ "prnt", "" }, new Object[]{ "prnt", "Today" }, new Object[]{ "prnt", " " }, new Object[]{ "prnt", "is" }, new Object[]{ "prnt", " " }, new Object[]{ "sbst", "%%DATE" }, new Object[]{ "prnt", "" }, new Object[]{ "prnt", "" } } },
            { "seq", new Object[]{ new Object[]{ "prnt", "" }, new Object[]{ "sbst", "%%DOCNAME" }, new Object[]{ "prnt", " " }, new Object[]{ "prnt", "written" }, new Object[]{ "prnt", " " }, new Object[]{ "prnt", "by" }, new Object[]{ "prnt", " " }, new Object[]{ "sbst", "%%AUTHOR" }, new Object[]{ "prnt", " " }, new Object[]{ "prnt", "has" }, new Object[]{ "prnt", " " }, new Object[]{ "prnt", "been" }, new Object[]{ "prnt", " " }, new Object[]{ "prnt", "published" }, new Object[]{ "prnt", "" }, new Object[]{ "prnt", "" } } },
        }, list.toArray());
    }

    @Test
    public void testFile() {
        final Object[][] tests = {
            strToCharArray(
                "%%! Here is a very primitive string templating\n" +
                "%%defn CLASS Foo\n" +
                "[ %%CLASS : %%UNDEFED ]: Error occurred!\n" +
                "    For more details, please realize this is made up..."),
        };

        final List<Object> list = new ArrayList<>();
        final Rulesets rsets = newExf();
        for (final Object[] test : tests) {
            final Object obj = rsets.getRule("File").apply(test);
            if (obj != null) {
                list.add(obj);
            }
        }

        // Perform actual macro expansion
        final StringBuilder text = new StringBuilder();
        for (Object r : list) expandMacro(text, new HashMap<>(), (Object[]) r);

        assertEquals(
                "[ Foo :  ]: Error occurred!\n" +
                "    For more details, please realize this is made up...",
                text.toString());
    }

    private static void expandMacro(final StringBuilder text, final Map<String, Object> names, final Object[] arr) {
        switch ((String) arr[0]) {
            case "seq": {
                final Object[] data = ((Object[]) arr[1]);
                for (Object r : data) expandMacro(text, names, (Object[]) r);
                break;
            }
            case "scom":
            case "mcom":
                // Ignore comments
                break;
            case "defn":
                // define name without trailing newline
                names.put((String) arr[1], new Object[]{ "prnt", ((String) arr[2]).replaceAll("\n$", "") });
                break;
            case "dexp":
                // define name as a expression template
                names.put((String) arr[1], arr[2]);
                break;
            case "udef":
                // undefine name
                names.remove((String) arr[1]);
                break;
            case "prnt":
                // output to text
                text.append(arr[1]);
                break;
            case "sbst": {
                // output substitution
                Object subst = names.get(arr[1]);
                if (subst == null) {
                    switch ((String) arr[1]) {
                        case "%%TIME":
                            subst = LocalTime.now().toString();
                            break;
                        case "%%DATE":
                            subst = LocalDate.now().toString();
                            break;
                        case "%%TIMESTAMP":
                            subst = LocalDateTime.now().toString();
                            break;
                        default:
                            // Not a special macro, give empty string
                            subst = "";
                            break;
                    }
                } else if (subst.getClass().isArray()) {
                    // Expand it, it is a macro expression
                    final StringBuilder sb = new StringBuilder();
                    expandMacro(sb, names, (Object[]) subst);
                    subst = sb;
                }
                text.append(subst);
                break;
            }
            default:
                throw new RuntimeException("Unknown directive " + arr[0]);
        }
    }
}
