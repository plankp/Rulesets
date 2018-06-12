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
          .append("public class ").append(className)
          .append("\n{\npublic EvalState state = new EvalState();\n")
          .append("public Extensions ext = new Extensions();\n");
        rsets.forEach(r -> {
            sb.append(rw.visit(r)).append('\n') // test{Name}
              .append(aw.visit(r)).append('\n') // act{Name}
              .append("public Object rule").append(r.name.getText()).append("(Object... data) {\n")
              .append("  state.reset(); state.setData(data);\n")
              .append("  final Map<String, Object> env = ext.export();\n")
              .append("  if (test").append(r.name.getText()).append("(env)) {\n")
              .append("    return act").append(r.name.getText()).append("(env);\n")
              .append("  }\n")
              .append("  return null;\n")
              .append("}\n");
        });
        sb.append("}");
        return sb.toString();
    }
}