package com.ymcmp.rset.tree;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import com.ymcmp.lexparse.tree.Visitor;
import com.ymcmp.lexparse.tree.ParseTree;

public class JavaRuleWriter extends Visitor<String> {

    private long varloop;
    private long varmerge;
    private long label;

    private int indent;

    private String fillIndent() {
        final char[] c = new char[indent * 4];
        Arrays.fill(c, ' ');
        return new String(c);
    }

    public String visitMethodNotFound(final ParseTree tree) {
        throw new RuntimeException(tree.getClass().getSimpleName() + " cannot be converted to rule");
    }

    public String visitRefRule(final RefRule n) {
        final String txt = n.node.getText();
        final String keep = (this.varmerge - 1) > 0 ? "m" + (this.varmerge - 1) : "captures";
        final StringBuilder sb = new StringBuilder();
        sb.append(fillIndent()).append("{\n"); ++indent;
        sb.append(fillIndent()).append("result = test").append(txt).append("(").append(keep).append(");\n");
        --indent;
        sb.append(fillIndent()).append('}');
        return sb.toString();
    }

    public String visitValueNode(final ValueNode n) {
        switch (n.token.type) {
            case S_ST:  return fillIndent() + "{ result = state.testSlotOccupied(); }";
            case S_EX:  return fillIndent() + "{ result = state.testEnd(); }";
            default:    return fillIndent() + "{ result = state.testEquality(" + n.toJavaLiteral() + "); }";
        }
    }

    public String visitUnaryRule(final UnaryRule n) {
        switch (n.op.type) {
            case S_TD: {
                final StringBuilder sb = new StringBuilder();
                sb.append(fillIndent()).append("{\n"); ++indent;
                sb.append(visit(n.rule)).append('\n')
                  .append(fillIndent()).append("result = !result;\n"); --indent;
                sb.append(fillIndent()).append('}');
                return sb.toString();
            }
            case S_QM: {
                final StringBuilder sb = new StringBuilder();
                sb.append(fillIndent()).append("{\n"); ++indent;
                sb.append(fillIndent()).append("state.save();\n")
                  .append(visit(n.rule)).append('\n')
                  .append(fillIndent()).append("if (!result) state.unsave();\n")
                  .append(fillIndent()).append("result = true;\n"); --indent;
                sb.append(fillIndent()).append('}');
                return sb.toString();
            }
            case S_AD: {
                final long label = this.label++;
                final String varloop = "i" + this.varloop++;
                final StringBuilder sb = new StringBuilder();
                sb.append(fillIndent()).append('$').append(label)
                  .append(": for (long ").append(varloop).append(" = 0; ; ++").append(varloop).append(") {\n"); ++indent;
                sb.append(fillIndent()).append("state.save();\n")
                  .append(visit(n.rule)).append('\n')
                  .append(fillIndent()).append("if (!result) {\n"); ++indent;
                sb.append(fillIndent()).append("state.unsave();\n")
                  .append(fillIndent()).append("result = ").append(varloop).append(" != 0;\n")
                  .append(fillIndent()).append("break $").append(label).append(";\n"); --indent;
                sb.append(fillIndent()).append("}\n"); --indent;
                sb.append("// Deal with captures\n")
                  .append(fillIndent()).append('}');
                this.varloop--;
                return sb.toString();
            }
            case S_ST: {
                final long label = this.label++;
                final String varloop = "i" + this.varloop++;
                final StringBuilder sb = new StringBuilder();
                sb.append(fillIndent()).append('$').append(label)
                  .append(": for (long ").append(varloop).append(" = 0; ; ++").append(varloop).append(") {\n"); ++indent;
                sb.append(fillIndent()).append("state.save();\n")
                  .append(visit(n.rule)).append('\n')
                  .append(fillIndent()).append("if (!result) {\n"); ++indent;
                sb.append(fillIndent()).append("state.unsave();\n")
                  .append(fillIndent()).append("break $").append(label).append(";\n"); --indent;
                sb.append(fillIndent()).append("}\n"); --indent;
                sb.append("// Deal with captures\n")
                  .append(fillIndent()).append("}\n")
                  .append(fillIndent()).append("result = true;");
                this.varloop--;
                return sb.toString();
            }
            default:
                throw new RuntimeException("Unknown unary operator " + n.op);
        }
    }

    public String visitBinaryRule(final BinaryRule n) {
        switch (n.op.type) {
            case S_MN: {
                final Comparable a = (Comparable) ((ValueNode) n.rule1).toObject();
                final Comparable b = (Comparable) ((ValueNode) n.rule2).toObject();
                return fillIndent() + "{ result = state.testRange(" + a + ',' + b + "); }";
            }
            default:
                throw new RuntimeException("Unknown binary operator " + n.op);
        }
    }

    public String visitKaryRule(final KaryRule n) {
        switch (n.type) {
            case SEQ: {
                final long label = this.label++;
                final String varmerge = "m" + this.varmerge++;
                final StringBuilder sb = new StringBuilder();
                sb.append(fillIndent()).append('$').append(label)
                  .append(": while (true) {\n"); ++indent;
                sb.append(fillIndent()).append("final Map<String, Object> ").append(varmerge).append(" = new HashMap<>();\n");
                n.rules.stream().map(this::visit).forEach(line -> {
                    sb.append(line).append('\n')
                      .append(fillIndent()).append("if (!result) break $").append(label).append(";\n")
                      .append(fillIndent()).append(varmerge).append(".putAll(captures);\n");
                });
                sb.append(fillIndent()).append("captures.putAll(").append(varmerge).append(");\n")
                  .append(fillIndent()).append("result = true;\n")
                  .append(fillIndent()).append("break;\n"); --indent;
                sb.append(fillIndent()).append('}');
                this.varmerge--;
                return sb.toString();
            }
            case SWITCH: {
                final long label = this.label++;
                final StringBuilder sb = new StringBuilder();
                sb.append(fillIndent()).append('$').append(label)
                  .append(": while (true) {\n"); ++indent;
                n.rules.stream().map(this::visit).forEach(line -> {
                    sb.append(fillIndent()).append("state.save();\n")
                      .append("// Do we clear captures here?\n")
                      .append(line).append('\n')
                      .append(fillIndent()).append("if (result) {\n"); ++indent;
                    sb.append(fillIndent()).append("break $").append(label).append(";\n"); --indent;
                    sb.append(fillIndent()).append("}\n")
                      .append(fillIndent()).append("state.unsave();\n");
                });
                sb.append(fillIndent()).append("result = false;\n")
                  .append(fillIndent()).append("break;\n"); --indent;
                sb.append(fillIndent()).append('}');
                return sb.toString();
            }
            default:
                throw new RuntimeException("Unknown rule block " + n.type);
        }
    }

    public String visitCaptureRule(final CaptureRule n) {
        final String dest = n.dest.getText();
        final StringBuilder sb = new StringBuilder();
        sb.append(fillIndent()).append("{ // Capturing " + dest + "\n"); ++indent;
        sb.append(fillIndent()).append("final int src = state.getIndex();\n")
          .append(visit(n.rule)).append('\n')
          .append(fillIndent()).append("final Object[] capture = result ? state.copyFromIndex(src) : new Object[0];\n")
          .append(fillIndent()).append("captures.put(\"").append(dest).append("\",capture);\n");
        --indent;
        sb.append(fillIndent()).append('}');
        return sb.toString();
    }

    public String visitRulesetNode(final RulesetNode n) {
        final StringBuilder sb = new StringBuilder();
        sb.append(fillIndent()).append("public boolean test").append(n.name.getText()).append("(final Map<String, Object> env) {\n"); ++indent;
        sb.append(fillIndent()).append("boolean result;\n")
          .append(fillIndent()).append("Map<String, Object> captures = new HashMap<>();\n")
          .append(visit(n.rule)).append('\n')
          .append(fillIndent()).append("if (env != null) env.putAll(captures);\n")
          .append(fillIndent()).append("return result;\n"); --indent;
        sb.append('}');
        return sb.toString();
    }
}