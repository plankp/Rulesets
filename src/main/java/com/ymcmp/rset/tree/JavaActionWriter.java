package com.ymcmp.rset.tree;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import java.util.stream.Stream;
import java.util.stream.Collectors;

import java.util.function.Function;
import java.util.function.Supplier;

import com.ymcmp.rset.lib.Stdlib;
import com.ymcmp.rset.lib.Mathlib;

import com.ymcmp.lexparse.tree.Visitor;
import com.ymcmp.lexparse.tree.ParseTree;

public class JavaActionWriter extends Visitor<String> {

    public static final String FN_TYPE = "Function<Map<String, Object>, Object>";

    private long varenv;

    private int indent;

    private String fillIndent() {
        final char[] c = new char[indent * 4];
        Arrays.fill(c, ' ');
        return new String(c);
    }

    public String visitMethodNotFound(final ParseTree tree) {
        throw new RuntimeException(tree.getClass().getSimpleName() + " cannot be converted to action");
    }

    public String visitValueNode(ValueNode n) {
        final String env = "e" + varenv++;
        return "((" + FN_TYPE + ")(" + env + " -> " + n.toJavaLiteral() + "))";
    }

    public String visitUnaryRule(UnaryRule n) {
        final String env = "e" + varenv++;
        final String ref = visit(n.rule);
        switch (n.op.type) {
            case S_QM:
                return "((" + FN_TYPE + ")(" + env + " -> " + env + ".get(" + ref + ".apply(" + env + ").toString())))";
            default:
                throw new RuntimeException("Unknown unary operator " + n.op);
        }
    }

    public String visitBinaryRule(BinaryRule n) {
        final String env = "e" + varenv++;
        final String lhs = visit(n.rule1);
        final String rhs = visit(n.rule2);
        switch (n.op.type) {
            case S_AD:
                return "((" + FN_TYPE + ")(" + env + " -> {" +
                    "final Number a = (Number) " + lhs + ".apply(" + env + ");" +
                    "final Number b = (Number) " + rhs + ".apply(" + env + ");" +
                    "return Mathlib.add(a.doubleValue(), b.doubleValue());" +
                "}))";
            case S_MN:
                return "((" + FN_TYPE + ")(" + env + " -> {" +
                    "final Number a = (Number) " + lhs + ".apply(" + env + ");" +
                    "final Number b = (Number) " + rhs + ".apply(" + env + ");" +
                    "return Mathlib.sub(a.doubleValue(), b.doubleValue());" +
                "}))";
            case S_ST:
                return "((" + FN_TYPE + ")(" + env + " -> {" +
                    "final Number a = (Number) " + lhs + ".apply(" + env + ");" +
                    "final Number b = (Number) " + rhs + ".apply(" + env + ");" +
                    "return Mathlib.mul(a.doubleValue(), b.doubleValue());" +
                "}))";
            case S_DV:
                return "((" + FN_TYPE + ")(" + env + " -> {" +
                    "final Number a = (Number) " + lhs + ".apply(" + env + ");" +
                    "final Number b = (Number) " + rhs + ".apply(" + env + ");" +
                    "return Mathlib.div(a.doubleValue(), b.doubleValue());" +
                "}))";
            case S_MD:
                return "((" + FN_TYPE + ")(" + env + " -> {" +
                    "final Number a = (Number) " + lhs + ".apply(" + env + ");" +
                    "final Number b = (Number) " + rhs + ".apply(" + env + ");" +
                    "return Mathlib.mod(a.doubleValue(), b.doubleValue());" +
                "}))";
            default:
                throw new RuntimeException("Unknown binary operator " + n.op);
        }
    }

    public String visitKaryRule(final KaryRule n) {
        final String env = "e" + varenv++;
        final String rules = n.rules.stream().map(this::visit)
                .collect(Collectors.joining(", "));
        switch (n.type) {
            case SUBSCRIPT:
                return "((" + FN_TYPE + ")(" + env + " -> Stream.of(" + rules + ").map(e -> e.apply(" + env + "))" +
                        ".reduce(Stdlib::subscript)" +
                        ".orElse(null)))";
            case JOIN:
                return "((" + FN_TYPE + ")(" + env + " -> Stdlib.concat(Stream.of(" + rules + ").map(e -> e.apply(" + env + "))" +
                        ".toArray(Object[]::new))))";
            case ARRAY:
                return "((" + FN_TYPE + ")(" + env + " -> Stream.of(" + rules + ").map(e -> e.apply(" + env + ")).toArray()))";
            case CALL:
                return "((" + FN_TYPE + ")(" + env + " -> {" +
                    "final Object[] arr = Stream.of(" + rules + ").map(e -> e.apply(" + env + ")).toArray(Object[]::new);" +
                    "final Object[] args = new Object[arr.length - 1];" +
                    "System.arraycopy(arr, 1, args, 0, args.length);" +
                    "return ((" + FN_TYPE + "<Object[], ?>) arr[0]).apply(args);" +
                "}))";
            case AND:
                return "((" + FN_TYPE + ")(" + env + " -> {" +
                    "Object u = null;" +
                    "final " + FN_TYPE + "[] elms = {" + rules + "};" +
                    "for (int i = 0; i < elms.length; ++i) {" +
                        "u = elms[i].apply(" + env + ");" +
                        "if (!Stdlib.isTruthy(u)) return u;" +
                    "}" +
                    "return u;" +
                "}))";
            case OR:
                return "((" + FN_TYPE + ")(" + env + " -> {" +
                    "Object u = null;" +
                    "final " + FN_TYPE + "[] elms = {" + rules + "};" +
                    "for (int i = 0; i < elms.length; ++i) {" +
                        "u = elms[i].apply(" + env + ");" +
                        "if (Stdlib.isTruthy(u)) return u;" +
                    "}" +
                    "return u;" +
                "}))";
            case ASSIGN:
                // NOTE: This implementation does not mutate maps or arrays
                return "((" + FN_TYPE + ")(" + env + " -> {" +
                    "Object u = elms[0].apply(" + env + ");" +
                    "final " + FN_TYPE + "[] elms = {" + rules + "};" +
                    "for (int i = 1; i < elms.length; ++i) {" +
                        "final Object k = elms[i].apply(" + env + ");" +
                        "" + env + ".put(u.toString(), k);" +
                        "u = k;" +
                    "}" +
                    "return u;" +
                "}))";
            default:
                throw new RuntimeException("Unknown rule block " + n.type);
        }
    }

    public String visitRulesetNode(final RulesetNode n) {
        final StringBuilder sb = new StringBuilder();
        sb.append(fillIndent()).append("public Object act").append(n.name.getText()).append("(Map<String, Object> env) {\n"); ++indent;
        if (n.expr == null) {
            sb.append(fillIndent()).append("return null;\n");
        } else {
            sb.append(fillIndent()).append("final " + FN_TYPE + " f = ").append(visit(n.expr)).append(";\n")
              .append(fillIndent()).append("return f.apply(env);\n");
        }
        --indent;
        sb.append('}');
        return sb.toString();
    }
}