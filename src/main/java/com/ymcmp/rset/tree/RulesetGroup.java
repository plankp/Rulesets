package com.ymcmp.rset.tree;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import com.ymcmp.rset.Ruleset;

import com.ymcmp.lexparse.tree.ParseTree;

public final class RulesetGroup extends ParseTree {

    public List<RulesetNode> rsets;

    public RulesetGroup(List<RulesetNode> rsets) {
        this.rsets = rsets;
    }

    @Override
    public ParseTree getChild(int node) {
        return rsets.get(node);
    }

    @Override
    public int getChildCount() {
        return rsets.size();
    }

    @Override
    public String getText() {
        return '(' + "begin " + rsets.stream().map(ParseTree::getText)
                .collect(Collectors.joining(" ")) + ')';
    }

    public Stream<Ruleset> toRulesetStream() {
        return rsets.stream().map(RulesetNode::toRuleset);
    }

    public String toJavaCode(final String className) {
        final JavaRuleWriter rw = new JavaRuleWriter();
        final JavaActionWriter aw = new JavaActionWriter();
        final StringBuilder sb = new StringBuilder();
        sb.append("import java.util.Map;\n")
          .append("import java.util.HashMap;\n")
          .append("import java.util.stream.Stream;\n")
          .append("import java.util.function.Function;\n\n")
          .append("import com.ymcmp.rset.EvalState;\n")
          .append("import com.ymcmp.rset.lib.Stdlib;\n")
          .append("import com.ymcmp.rset.lib.Mathlib;\n")
          .append("import com.ymcmp.rset.lib.Extensions;\n\n")
          .append("public class ").append(className).append("\n{\n")
          .append("@FunctionalInterface\n")
          .append("public static interface Action extends Function<Map<String, Object>, Object> {}\n")
          .append("@FunctionalInterface\n")
          .append("public static interface Rule extends Function<Object[], Object> {}\n")
          .append("public EvalState state = new EvalState();\n")
          .append("public Extensions ext = new Extensions();\n");
        final StringBuilder ruleList = new StringBuilder();
        rsets.forEach(r -> {
            final String ruleName = "rule" + r.name.getText(); // rule{Name}
            sb.append(rw.visit(r)).append('\n') // test{Name}
              .append(aw.visit(r)).append('\n') // act{Name}
              .append("public Object ").append(ruleName).append("(Object... data) {\n")
              .append("  state.reset(); state.setData(data);\n")
              .append("  final Map<String, Object> env = ext.export();\n")
              .append("  if (test").append(r.name.getText()).append("(env)) {\n")
              .append("    return act").append(r.name.getText()).append("(env);\n")
              .append("  }\n")
              .append("  return null;\n")
              .append("}\n\n");
            ruleList
                .append("  put(\"").append(r.name.getText()).append("\", ")
                .append(className).append(".this::").append(ruleName).append(");\n");
        });
        sb.append("public final Map<String, Rule> rules = new HashMap<String, Rule>() {{\n").append(ruleList).append("}};\n");
        sb.append("}");
        return sb.toString();
    }
}