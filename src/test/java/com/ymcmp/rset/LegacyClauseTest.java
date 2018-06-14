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

public class LegacyClauseTest {

    private static Class<?> LegacyClause;
    private static Extensions ext;

    @BeforeClass
    public static void compile() {
        final StringReader reader = new StringReader(
            "# A bunch of rule clauses follow...\n" +
            "rule alpha = 0 abc | (1!) { ?_ ((1 + 2 + 3) & abc | def) },\n" +
            "subrule b  = 00 (abc | 1),\n" +
            "rule all   = ((*) (*) | (*))! { 'Kleen AF' },\n" +
            "rule inc   = &b abc,\n" +
            "rule abc   = a b c { 'rule abc is matched' },\n" +
            "rule a2f   = r:&abc d e f:f { ?_join You can count from ?r:0 to ?f }"
        );

        final RsetLexer lexer = new RsetLexer(reader);
        final RsetParser parser = new RsetParser(lexer);
        final byte[] bytes = parser.parse().toBytecode("LegacyClause");
        final ByteClassLoader bcl = new ByteClassLoader();
        final Class<?> cl = bcl.loadFromBytes("LegacyClause", bytes);
        if (Rulesets.class.isAssignableFrom(cl)) {
            LegacyClause = cl;
        } else {
            throw new RuntimeException("This should not happen, generated classes must inherit Rulesets");
        }

        ext = new Extensions();
    }

    public static Rulesets newLegacyClause() {
        try {
            // Calling soft dependency constructor
            return (Rulesets) LegacyClause.getConstructor(Extensions.class).newInstance(ext);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testRules() {
        final Object[][] tests = {
            { 0, "abc" },
            { 0, 1 },
            { 1 },
            { 0, 1, "abc" },
            { "a", "b", "c", },
            { "a", "b", "c", "d", "e", "f" },
        };

        final StringBuilder sb = new StringBuilder();
        for (final Object[] test : tests) {
            newLegacyClause().forEachRule((name, rule) -> {
                final Object obj = rule.apply(test);
                if (obj != null) {
                    sb.append(name).append(',').append(obj).append('\n');
                }
            });
        }
        assertEquals(
                "all,Kleen AF\n" +
                "alpha,abc\n" +
                "all,Kleen AF\n" +
                "all,Kleen AF\n" +
                "alpha,abc\n" +
                "abc,rule abc is matched\n" +
                "abc,rule abc is matched\n" +
                "a2f,You can count from a to f\n",
                sb.toString());
    }
}