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

public class QuadraticTest {

    private static Class<?> Quadratic;

    private Rulesets rsets;

    @BeforeClass
    public static void compile() {
        final StringReader reader = new StringReader(
            "# Is this considered a meta-lexer?\n" +
            "fragment ws = (' ' | '\t' | '\r' | '\n')*,\n" +
            "rule number = ('0' | '1'-'9' ('0'-'9')*) ('.' '0'-'9'*)?,\n" +
            "rule calc   = &ws\n" +
            "  t1:(s1:('+'|'-')? &ws n1:&number? &ws x &ws '^' &ws '2')? &ws\n" +
            "  t2:(s2:('+'|'-') &ws n2:&number? &ws x)? &ws\n" +
            "  (s3:('+'|'-') &ws n3:&number)? &ws\n" +
            "{\n" +
            "   a=(?_float (?s1|'')~(?n1 | (?t1 & 1 | 0))):0;\n" +
            "   b=(?_float (?s2|'')~(?n2 | (?t2 & 1 | 0))):0;\n" +
            "   c=(?_float (?s3 | '') ~ (?n3 | 0)):0;\n" +
            "   ((0 - ?b) + (?_sqrt (?_pow ?b 2) - 4 * ?a * ?c)) / (2 * ?a) ~" +
            "   ',' ~ ((0 - ?b) - (?_sqrt (?_pow ?b 2) - 4 * ?a * ?c)) / (2 * ?a)" +
            "}"
        );

        final RsetLexer lexer = new RsetLexer(reader);
        final RsetParser parser = new RsetParser(lexer);
        final byte[] bytes = parser.parse().toBytecode("Quadratic");
        final ByteClassLoader bcl = new ByteClassLoader();
        final Class<?> cl = bcl.loadFromBytes("Quadratic", bytes);
        if (Rulesets.class.isAssignableFrom(cl)) {
            Quadratic = cl;
        } else {
            throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
        }
    }

    public static Rulesets newQuadratic() {
        try {
            return (Rulesets) Quadratic.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Before
    public void initializeRuleset() {
        rsets = newQuadratic();
    }

    @Test
    public void testEmpty() {
        final Object[][] tests = {
            { }
        };

        final StringBuilder sb = new StringBuilder();
        for (final Object[] test : tests) {
            rsets.forEachRule((name, rule) -> {
                final Object obj = rule.apply(test);
                if (obj != null) {
                    sb.append(name).append(',').append(obj).append('\n');
                }
            });
        }
        assertEquals("calc,NaN,NaN\n",
                sb.toString());
    }

    @Test
    public void testSpotA() {
        final Object[][] tests = {
            { "2", " ", "x", " ", "^", " ", "2" },
            { "3", ".", "1", "4", "x", "^", "2" },
            { "-", "0", ".", "6", "x", "^", "2" },
        };

        final StringBuilder sb = new StringBuilder();
        for (final Object[] test : tests) {
            rsets.forEachRule((name, rule) -> {
                final Object obj = rule.apply(test);
                if (obj != null) {
                    sb.append(name).append(',').append(obj).append('\n');
                }
            });
        }
        assertEquals("calc,0,0\ncalc,0,0\ncalc,0,0\n",
                sb.toString());
    }

    @Test
    public void testSpotABC() {
        final Object[][] tests = {
            { "1", "0", "x", "^", "2", "+", "x", "-", "2" },
            { "x", "^", "2", "-", "2", "x", "-", "3", "5" },
        };

        final StringBuilder sb = new StringBuilder();
        for (final Object[] test : tests) {
            rsets.forEachRule((name, rule) -> {
                final Object obj = rule.apply(test);
                if (obj != null) {
                    sb.append(name).append(',').append(obj).append('\n');
                }
            });
        }
        assertEquals("calc,0.4,-0.5\ncalc,7,-5\n",
                sb.toString());
    }
}