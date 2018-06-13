package com.ymcmp.rset;

import java.io.IOException;
import java.io.StringReader;

import java.lang.reflect.InvocationTargetException;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ymcmp.rset.rt.Rulesets;
import com.ymcmp.rset.lib.Extensions;
import com.ymcmp.rset.tree.RulesetGroup;

public class Main {

    public static void main(String[] args) {
        final StringReader reader = new StringReader(
            "# A bunch of rule clauses follow...\n" +
            "rule alpha = 0 abc | (1!) { ?_ ((1 + 2 + 3) & abc | def) },\n" +
            "rule beta  = 00 (abc | 1),\n" +
            "rule all   = ((*) (*) | (*))! { 'Kleen AF' },\n" +
            "rule inc   = &beta abc,\n" +
            "rule abc   = a b c { 'rule abc is matched' },\n" +
            "rule a2f   = r:&abc d e f:f { ?_join You can count from ?r:0 to ?f }"
        );
        try (final RsetLexer lexer = new RsetLexer(reader)) {
            final RsetParser parser = new RsetParser(lexer);
            final RulesetGroup tree = parser.parse();
            final byte[] bytes = tree.toBytecode("CompiledRules");

            final Object[][] tests = {
                { 0, "abc" },
                { 0, 1 },
                { 1 },
                { 0, 1, "abc" },
                { "a", "b", "c", },
                { "a", "b", "c", "d", "e", "f" },
            };

            // // Uncomment out these lines to check the class file!
            // try (java.io.FileOutputStream fos = new java.io.FileOutputStream("CompiledRules.class")) {
            //     fos.write(bytes);
            //     fos.flush();
            // }

            final ByteClassLoader bcl = new ByteClassLoader();
            final Class<?> cl = bcl.loadFromBytes("CompiledRules", bytes);
            if (Rulesets.class.isAssignableFrom(cl)) {
                final Rulesets rulesets = (Rulesets) cl.getConstructor().newInstance();
                for (final Object[] test : tests) {
                    rulesets.forEachRule((name, rule) -> {
                        final Object obj = rule.apply(test);
                        if (obj != null) {
                            if (obj.getClass().isArray()) {
                                System.out.println("rule " + name + " --> " + Arrays.deepToString((Object[]) obj));
                            } else {
                                System.out.println("rule " + name + " --> " + obj);
                            }
                        }
                    });
                }
            } else {
                System.out.println("Wtf!??? Class must inherit rt.Rulesets");
            }
        } catch (IOException ex) {
            //
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            System.out.println("Welp... " + ex);
        }
    }
}