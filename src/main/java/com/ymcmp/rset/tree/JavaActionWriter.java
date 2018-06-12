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

public class JavaActionWriter extends Visitor<Data> {

    public static final String FN_TYPE = "Action";

    private long varenv;
    private long varobj;
    private long varelms;

    private int indent;

    private String fillIndent() {
        final char[] c = new char[indent * 4];
        Arrays.fill(c, ' ');
        return new String(c);
    }

    public Data visitMethodNotFound(final ParseTree tree) {
        throw new RuntimeException(tree.getClass().getSimpleName() + " cannot be converted to action");
    }

    public Data visitValueNode(ValueNode n) {
        final String s = n.toJavaLiteral();
        return new Data(s.charAt(0) == '"' ? Data.Type.STRING : Data.Type.NUMBER, s);
    }

    public Data visitUnaryRule(UnaryRule n) {
        final Data ref = visit(n.rule);
        switch (n.op.type) {
            case S_QM: {
                final String env = "e" + varenv++;
                switch (ref.type) {
                    case NUMBER:
                        return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> " + env + ".get(String.valueOf(" + ref + "))))");
                    case STRING:
                        return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> " + env + ".get(" + ref + ")))");
                    default:
                        return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> " + env + ".get(" + ref + ".apply(" + env + ").toString())))");
                }
            }
            default:
                throw new RuntimeException("Unknown unary operator " + n.op);
        }
    }

    public Data visitBinaryRule(BinaryRule n) {
        final String env = "e" + varenv++;
        final Data lhs = visit(n.rule1);
        final Data rhs = visit(n.rule2);

        boolean compileTime = true;

        // Assumes all math equations down the road
        final String lineA;
        switch (lhs.type) {
            case NUMBER:
                lineA = lhs.text;
                break;
            case STRING:
                lineA = Double.valueOf(lhs.text).toString();
                break;
            case FUNC:
                lineA = "((Number) " + lhs + ".apply(" + env + ")).doubleValue()";
                compileTime = false;
                break;
            default:
                throw new RuntimeException("Illegal context of lhs:" + lhs.type + " at " + n.op.type);
        }

        final String lineB;
        switch (rhs.type) {
            case NUMBER:
                lineB = rhs.text;
                break;
            case STRING:
                lineB = Double.valueOf(rhs.text).toString();
                break;
            case FUNC:
                lineB = "((Number) " + rhs + ".apply(" + env + ")).doubleValue()";
                compileTime = false;
                break;
            default:
                throw new RuntimeException("Illegal context of rhs:" + rhs.type + " at " + n.op.type);
        }
        switch (n.op.type) {
            case S_AD:
                if (compileTime) {
                    --varenv;
                    return new Data(Data.Type.NUMBER, Double.parseDouble(lineA) + Double.parseDouble(lineB));
                } else {
                    return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> Mathlib.add(lineA, lineB)))");
                }
            case S_MN:
                if (compileTime) {
                    --varenv;
                    return new Data(Data.Type.NUMBER, Double.parseDouble(lineA) - Double.parseDouble(lineB));
                } else {
                    return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> Mathlib.sub(lineA, lineB)))");
                }
            case S_ST:
                if (compileTime) {
                    --varenv;
                    return new Data(Data.Type.NUMBER, Double.parseDouble(lineA) * Double.parseDouble(lineB));
                } else {
                    return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> Mathlib.mul(lineA, lineB)))");
                }
            case S_DV:
                if (compileTime) {
                    --varenv;
                    return new Data(Data.Type.NUMBER, Double.parseDouble(lineA) / Double.parseDouble(lineB));
                } else {
                    return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> Mathlib.div(lineA, lineB)))");
                }
            case S_MD:
                if (compileTime) {
                    --varenv;
                    return new Data(Data.Type.NUMBER, Double.parseDouble(lineA) % Double.parseDouble(lineB));
                } else {
                    return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> Mathlib.mod(lineA, lineB)))");
                }
            default:
                throw new RuntimeException("Unknown binary operator " + n.op);
        }
    }

    public Data visitKaryRule(final KaryRule n) {
        final String env = "e" + varenv++;
        final Data[] rules = n.rules.stream().map(this::visit).toArray(Data[]::new);
        switch (n.type) {
            case SUBSCRIPT:
                return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> Stream.of(" + Stream.of(rules).map(e -> {
                    return (e.type == Data.Type.FUNC) ? e.text + ".apply(" + env + ")" : e.text;
                }).collect(Collectors.joining(", ")) + ")" +
                        ".reduce(Stdlib::subscript)" +
                        ".orElse(null)))");
            case JOIN:
                return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> Stdlib.concat(Stream.of(" + Stream.of(rules).map(e -> {
                    return (e.type == Data.Type.FUNC) ? e.text + ".apply(" + env + ")" : e.text;
                }).collect(Collectors.joining(", ")) + ").toArray())))");
            case ARRAY:
                return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> Stream.of(" + Stream.of(rules).map(e -> {
                    return (e.type == Data.Type.FUNC) ? e.text + ".apply(" + env + ")" : e.text;
                }).collect(Collectors.joining(", ")) + ").toArray()))");
            case CALL: {
                final String elms = "a" + varelms++;
                final String args = "a" + varelms++;
                return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> {" +
                    "final Object[] " + elms + " = Stream.of(" + Stream.of(rules).map(e -> {
                        return (e.type == Data.Type.FUNC) ? e.text + ".apply(" + env + ")" : e.text;
                    }).collect(Collectors.joining(", ")) + ").toArray();" +
                    "final Object[] " + args + " = new Object[" + elms + ".length - 1];" +
                    "System.arraycopy(" + elms + ", 1, " + args + ", 0, " + args + ".length);" +
                    "return ((Function<Object[], ?>) " + elms + "[0]).apply(" + args + ");" +
                "}))");
            }
            case AND: {
                final String elms = "a" + varelms++;
                final String u = "u" + varobj++;
                final String k = "u" + varobj++;
                return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> {" +
                    "Object " + u + " = null;" +
                    "final Object[] " + elms + " = {" + Stream.of(rules).map(Data::toString)
                            .collect(Collectors.joining(", ")) + "};" +
                    "for (int i = 0; i < " + elms + ".length; ++i) {" +
                        "final Object " + k + " = " + elms + "[i];" +
                        u + " = (" + k + " instanceof " + FN_TYPE + ") ? ((" + FN_TYPE + ") " + k + ").apply(" + env + ") : " + k + ";" +
                        "if (!Stdlib.isTruthy(" + u + ")) return " + u + ";" +
                    "}" +
                    "return " + u + ";" +
                "}))");
            }
            case OR: {
                final String elms = "a" + varelms++;
                final String u = "u" + varobj++;
                final String k = "u" + varobj++;
                return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> {" +
                    "Object " + u + " = null;" +
                    "final Object[] " + elms + " = {" + Stream.of(rules).map(Data::toString)
                            .collect(Collectors.joining(", ")) + "};" +
                    "for (int i = 0; i < " + elms + ".length; ++i) {" +
                        "final Object " + k + " = " + elms + "[i];" +
                        u + " = (" + k + " instanceof " + FN_TYPE + ") ? ((" + FN_TYPE + ") " + k + ").apply(" + env + ") : " + k + ";" +
                        "if (Stdlib.isTruthy(" + u + ")) return " + u + ";" +
                    "}" +
                    "return " + u + ";" +
                "}))");
            }
            case ASSIGN: {
                // NOTE: This implementation does not mutate maps or arrays
                final String elms = "a" + varelms++;
                final String u = "u" + varobj++;
                final String k = "u" + varobj++;
                final String w = "u" + varobj++;
                return new Data(Data.Type.FUNC, "((" + FN_TYPE + ")(" + env + " -> {" +
                    "final Object[] " + elms + " = {" + Stream.of(rules).map(Data::toString)
                            .collect(Collectors.joining(", ")) + "};" +
                    "Object " + u + " = null;" +
                    "for (int i = 0; i < " + elms + ".length; ++i) {" +
                        "final Object " + k + " = " + elms + "[i];" +
                        "final Object " + w + " = (" + k + " instanceof " + FN_TYPE + ") ? ((" + FN_TYPE + ") " + k + ").apply(" + env + ") : " + k + ";" +
                        "if (i != 0)" + env + ".put(" + u + ".toString(), " + w + ");" +
                        u + " = " + w + ";" +
                    "}" +
                    "return " + u + ";" +
                "}))");
            }
            default:
                throw new RuntimeException("Unknown rule block " + n.type);
        }
    }

    public Data visitRulesetNode(final RulesetNode n) {
        final StringBuilder sb = new StringBuilder();
        sb.append(fillIndent()).append("public Object act").append(n.name.getText()).append("(Map<String, Object> env) {\n"); ++indent;
        if (n.expr == null) {
            sb.append(fillIndent()).append("return null;\n");
        } else {
            final Data data = visit(n.expr);
            switch (data.type) {
                case FUNC:
                    sb.append(fillIndent()).append("return ").append(data).append(".apply(env);\n");
                    break;
                default:
                    sb.append(fillIndent()).append("return ").append(data).append(";\n");
                    break;
            }
        }
        --indent;
        sb.append('}');
        return new Data(Data.Type.ROOT, sb.toString());
    }
}

final class Data {

    public enum Type {
        NUMBER, STRING, FUNC, ROOT;
    }

    public final Type type;
    public final String text;

    public Data(Type type, String text) {
        this.type = type;
        this.text = text;
    }

    public Data(Type type, double text) {
        this.type = type;
        this.text = String.valueOf(text);
    }

    public boolean isBasicValue() {
        switch (type) {
            case NUMBER:
            case STRING:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return text;
    }
}