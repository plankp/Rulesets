package com.ymcmp.rset;

import java.io.IOException;
import java.io.StringReader;

import java.lang.reflect.InvocationTargetException;

import java.util.Map;

import com.ymcmp.rset.rt.Rulesets;

import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

public class NumberLexerTest {

    private static Class<?> NumberLexer;

    @BeforeClass
    public static void compile() {
        final StringReader reader = new StringReader(
            "# Is this considered a meta-lexer?\n" +
            "rule binary = '0'-'1'+,\n" +
            "rule octal  = '0'-'7'+,\n" +
            "rule digit  = '0'-'9'+,\n" +
            "rule alhex  = (&digit | a-f | A-F)+,\n" +
            "rule n\n" +
            "  = n:('0' (x &alhex | b &binary | c &octal)?\n"+
            "  | '1'-'9' &digit?)\n" +
            "{ ?_concat ?n }"
        );

        final RsetLexer lexer = new RsetLexer(reader);
        final RsetParser parser = new RsetParser(lexer);
        final byte[] bytes = parser.parse().toBytecode("NumberLexer");
        final ByteClassLoader bcl = new ByteClassLoader();
        final Class<?> cl = bcl.loadFromBytes("NumberLexer", bytes);
        if (Rulesets.class.isAssignableFrom(cl)) {
            NumberLexer = cl;
        } else {
            throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
        }
    }

    public static Rulesets newNumberLexer() {
        try {
            return (Rulesets) NumberLexer.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testBase2() {
        final Object[][] tests = {
            { "0", "b", "0" }, { "0", "b", "1" },
            { "0", "b", "1", "0" },
            { "0", "b", "1", "0", "0", "1", "0" },
            { "0", "b", "0", "1", "0", "1", "1" },
        };

        final StringBuilder sb = new StringBuilder();
        for (final Object[] test : tests) {
            newNumberLexer().forEachRule((name, rule) -> {
                final Object obj = rule.apply(test);
                if (obj != null) {
                    sb.append(name).append(',').append(obj).append('\n');
                }
            });
        }
        assertEquals("n,0b0\nn,0b1\nn,0b10\nn,0b10010\nn,0b01011\n",
                sb.toString());
    }

    @Test
    public void testBase8() {
        final Object[][] tests = {
            { "0", "c", "0" }, { "0", "c", "1" }, { "0", "c", "2" },
            { "0", "c", "5" }, { "0", "c", "6" }, { "0", "c", "7" },
            { "0", "c", "1", "0" },
        };

        final StringBuilder sb = new StringBuilder();
        for (final Object[] test : tests) {
            newNumberLexer().forEachRule((name, rule) -> {
                final Object obj = rule.apply(test);
                if (obj != null) {
                    sb.append(name).append(',').append(obj).append('\n');
                }
            });
        }
        assertEquals("n,0c0\nn,0c1\nn,0c2\nn,0c5\nn,0c6\nn,0c7\nn,0c10\n",
                sb.toString());
    }

    @Test
    public void testBase10() {
        final Object[][] tests = {
            { "0" }, { "1" }, { "2" }, { "3" }, { "4" },
            { "5" }, { "6" }, { "7" }, { "8" }, { "9" },
            { "1", "0" },
        };

        final StringBuilder sb = new StringBuilder();
        for (final Object[] test : tests) {
            newNumberLexer().forEachRule((name, rule) -> {
                final Object obj = rule.apply(test);
                if (obj != null) {
                    sb.append(name).append(',').append(obj).append('\n');
                }
            });
        }
        assertEquals("n,0\nn,1\nn,2\nn,3\nn,4\nn,5\nn,6\nn,7\nn,8\nn,9\nn,10\n",
                sb.toString());
    }

    @Test
    public void testBase16() {
        final Object[][] tests = {
            // These will not match
            { "a" }, { "d" }, { "f" }, { "j" },
            { "A" }, { "D" }, { "F" }, { "J" },

            // These will match (will ignore the gh part of 0xfgh)
            { "0", "x", "a", "b", "c" },
            { "0", "x", "D", "E", "F" },
            { "0", "x", "f", "g", "h" },
            { "0", "x", "a", "C", "f" },
        };

        final StringBuilder sb = new StringBuilder();
        for (final Object[] test : tests) {
            newNumberLexer().forEachRule((name, rule) -> {
                final Object obj = rule.apply(test);
                if (obj != null) {
                    sb.append(name).append(',').append(obj).append('\n');
                }
            });
        }
        assertEquals("n,0xabc\nn,0xDEF\nn,0xf\nn,0xaCf\n",
                sb.toString());
    }
}