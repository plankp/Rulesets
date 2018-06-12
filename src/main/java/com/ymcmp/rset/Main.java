package com.ymcmp.rset;

import java.io.IOException;
import java.io.StringReader;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

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
            // "rule k = 1+ 2* 3 | j:~(5-6)? { Yes ~ ?j }"
        );
        try (final RsetLexer lexer = new RsetLexer(reader)) {
            final RsetParser parser = new RsetParser(lexer);
            final RulesetGroup tree = parser.parse();
            final Map<String, Ruleset> env = Ruleset.toEvalMap(tree.toRulesetStream());
            final Extensions ext = new Extensions();
            final Object[][] tests = {
                { 0, "abc" },
                { 0, 1 },
                { 1 },
                { 0, 1, "abc" },
                { "a", "b", "c", },
                { "a", "b", "c", "d", "e", "f" },
                // { 0, 2, 3, 4 }
            };
            for (final Object[] test : tests) {
                Ruleset.evalute(env, ext, test).forEach((name, u) -> {
                    u.ifPresent(obj -> {
                        if (obj.getClass().isArray()) {
                            System.out.println("rule " + name + " --> " + Arrays.deepToString((Object[]) obj));
                        } else {
                            System.out.println("rule " + name + " --> " + obj);
                        }
                    });
                });
            }

            System.out.println("Compiled as:");
            System.out.println(tree.toJavaCode("CompiledRules"));
            // System.out.println("As compiled code:");
            // final CompiledRules rules = new CompiledRules();
            // for (final Object[] test : tests) {
            //     rules.rules.forEach((name, rule) -> {
            //         final Object obj = rule.apply(test);
            //         if (obj != null) {
            //             if (obj.getClass().isArray()) {
            //                 System.out.println("rule " + name + " --> " + Arrays.deepToString((Object[]) obj));
            //             } else {
            //                 System.out.println("rule " + name + " --> " + obj);
            //             }
            //         }
            //     });
            // }
        } catch (IOException ex) {
            //
        }
    }
}