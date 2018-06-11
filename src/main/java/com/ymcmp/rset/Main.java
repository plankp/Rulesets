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

            // "# Is this considered a meta-lexer?\n" +
            // "rule digit    = n:('0'-'9'+),\n" +
            // "rule alhex    = n:((&digit | a-f | A-F)+),\n" +
            // "rule number   = n:('0' (x &alhex)? | '1'-'9' &digit?) { 'Yes: ' ~ (?_concat ?n) }"
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

                // { "x" },
                // { "0" }, { "1" }, { "2" }, { "3" }, { "4" },
                // { "5" }, { "6" }, { "7" }, { "8" }, { "9" },
                // boxChars("10"),

                // { "a" }, { "d" }, { "f" }, { "j" },
                // { "A" }, { "D" }, { "F" }, { "J" },
                // boxChars("0xabc"),
                // boxChars("0xDEF"),
                // boxChars("0xfgh"),
                // boxChars("0xaCf"),
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
        } catch (IOException ex) {
            //
        }
    }

    public static Character[] boxChars(final CharSequence cs) {
        return cs.chars().mapToObj(k -> (char) k).toArray(Character[]::new);
    }
}