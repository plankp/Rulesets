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

import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

public class ReadmeDemoTest {

    private static Class<?> ReadmeDemo;

    private Rulesets rsets;

    @BeforeClass
    public static void compile() {
        final StringReader reader = new StringReader(
            "# Matching numbers (an over-simplified lexer)\n" +
            "\n" +
            "fragment digit\n" +
            "  = %0-%9+\n" +
            ",\n" +
            "fragment alhex\n" +
            "  = (&digit | %a-%f | %A-%F)+\n" +
            ",\n" +
            "rule number\n" +
            "  = n:(%0 (%x &alhex)? | %1-%9 &digit?)\n" +
            "{\n" +
            "    'Yes: ' ~ (?_concat ?n)\n" +
            "}");

        final RsetLexer lexer = new RsetLexer(reader);
        final RsetParser parser = new RsetParser(lexer);
        final byte[] bytes = parser.parse().toBytecode("ReadmeDemo");
        final ByteClassLoader bcl = new ByteClassLoader();
        final Class<?> cl = bcl.loadFromBytes("ReadmeDemo", bytes);
        if (Rulesets.class.isAssignableFrom(cl)) {
            ReadmeDemo = cl;
        } else {
            throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
        }
    }

    public static Rulesets newReadmeDemo() {
        try {
            return (Rulesets) ReadmeDemo.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Before
    public void initializeRuleset() {
        rsets = newReadmeDemo();
    }

    private static Character[] strToCharArray(String str) {
        if (str == null || str.isEmpty()) return new Character[0];
        return str.chars().mapToObj(e -> (char) e).toArray(Character[]::new);
    }

    @Test
    public void testNumber() {
        final Object[][] tests = {
            strToCharArray("0"),
            strToCharArray("4"),
            strToCharArray("10"),
            strToCharArray("132"),

            strToCharArray("0x0"),
            strToCharArray("0xa"),
            strToCharArray("0xB8000"),
        };

        final StringBuilder sb = new StringBuilder();
        for (final Object[] test : tests) {
            rsets.forEachRule((name, rule) -> {
                final Object obj = rule.apply(test);
                if (obj != null) {
                    sb.append(obj).append('\n');
                }
            });
        }
        assertEquals(
                "Yes: 0\n" +
                "Yes: 4\n" +
                "Yes: 10\n" +
                "Yes: 132\n" +

                "Yes: 0x0\n" +
                "Yes: 0xa\n" +
                "Yes: 0xB8000\n",
                sb.toString());
    }
}