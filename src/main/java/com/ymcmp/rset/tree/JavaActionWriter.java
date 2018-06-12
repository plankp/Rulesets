package com.ymcmp.rset.tree;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import java.util.stream.Stream;
import java.util.stream.Collectors;

import com.ymcmp.rset.lib.Stdlib;
import com.ymcmp.rset.lib.Mathlib;

import com.ymcmp.lexparse.tree.Visitor;
import com.ymcmp.lexparse.tree.ParseTree;

public class JavaActionWriter extends Visitor<String> {

    public static final String FN_TYPE = "Action";

    private long varobj;

    public String visitMethodNotFound(final ParseTree tree) {
        throw new RuntimeException(tree.getClass().getSimpleName() + " cannot be converted to action");
    }

    public String visitValueNode(final ValueNode n) {
        final String s = n.toJavaLiteral();
        return s;
    }

    public String visitUnaryRule(final UnaryRule n) {
        final String ref = visit(n.rule);
        switch (n.op.type) {
            case S_QM:
                return "env.get(String.valueOf(" + ref + "))";
            default:
                throw new RuntimeException("Unknown unary operator " + n.op);
        }
    }

    public String visitBinaryRule(final BinaryRule n) {
        final String lhs = visit(n.rule1);
        final String rhs = visit(n.rule2);

        switch (n.op.type) {
            case S_AD:
                return "Mathlib.add(" + lhs + "," + rhs + ")";
            case S_MN:
                return "Mathlib.sub(" + lhs + "," + rhs + ")";
            case S_ST:
                return "Mathlib.mul(" + lhs + "," + rhs + ")";
            case S_DV:
                return "Mathlib.div(" + lhs + "," + rhs + ")";
            case S_MD:
                return "Mathlib.mod(" + lhs + "," + rhs + ")";
            default:
                throw new RuntimeException("Unknown binary operator " + n.op);
        }
    }

    public String visitKaryRule(final KaryRule n) {
        final String[] rules = n.rules.stream().map(this::visit).toArray(String[]::new);
        switch (n.type) {
            case SUBSCRIPT:
                return Stream.of(rules).reduce((a, b) -> "Stdlib.subscript(" + a + "," + b + ")").get();
            case JOIN:
                return Stream.of(rules).collect(Collectors.joining(",", "Stdlib.concat(", ")"));
            case ARRAY:
                return Stream.of(rules).collect(Collectors.joining(",", "new Object[]{", "}"));
            case CALL:
                return new StringBuilder()
                        .append("((Function<Object[], ?>)").append(rules[0]).append(").apply(")
                        .append(Stream.of(rules).skip(1).collect(Collectors.joining(",", "new Object[]{", "}")))
                        .append(')')
                        .toString();
            case AND: {
                final String u = "u" + this.varobj++;
                return Stream.of(rules).reduce((a, b) -> "(Stdlib.isTruthy(" + u + "=" + a + ")?" + b + ":" + u + ")").get();
            }
            case OR: {
                final String u = "u" + this.varobj++;
                return Stream.of(rules).reduce((a, b) -> "(Stdlib.isTruthy(" + u + "=" + a + ")?" + u + ":" + b + ")").get();
            }
            case ASSIGN: {
                // NOTE: This implementation does not mutate maps or arrays
                final String u = "u" + this.varobj++;
                return Stream.of(rules).reduce((a, b) -> "Stdlib.ignore(env.put(" + a + "," + u + "=" + b + "), " + u + ")").get();
            }
            default:
                throw new RuntimeException("Unknown rule block " + n.type);
        }
    }

    public String visitRulesetNode(final RulesetNode n) {
        final StringBuilder sb = new StringBuilder();
        sb.append("public Object act").append(n.name.getText()).append("(final Map<String, Object> env) {\n");
        for (int i = 0; i < this.varobj; ++i) {
            sb.append("    Object u").append(i).append(" = null;\n");
        }
        sb.append("    return ").append(n.expr == null ? "null" : visit(n.expr)).append(";\n").append('}');
        return sb.toString();
    }
}