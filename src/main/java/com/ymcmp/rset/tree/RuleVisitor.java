package com.ymcmp.rset.tree;

import java.util.Map;
import java.util.HashMap;

import com.ymcmp.rset.EvalRule;

import com.ymcmp.lexparse.tree.Visitor;
import com.ymcmp.lexparse.tree.ParseTree;

public class RuleVisitor extends Visitor<EvalRule> {

    public EvalRule visitMethodNotFound(final ParseTree tree) {
        throw new RuntimeException(tree.getClass().getSimpleName() + " cannot be converted to rule");
    }

    public EvalRule visitValueNode(ValueNode n) {
        switch (n.token.type) {
            case S_ST: return new EvalRule((self, env, state) -> state.testSlotOccupied());
            case S_EX: return new EvalRule((self, env, state) -> state.testEnd());
            default:
                final Object obj = n.toObject();
                return new EvalRule((self, env, state) -> {
                    return state.testEquality(obj);
                });
        }
    }

    public EvalRule visitUnaryRule(UnaryRule n) {
        final EvalRule base = visit(n.rule);
        switch (n.op.type) {
            case S_TD:
                return new EvalRule((self, env, state) -> {
                    return !base.eval(self, env, state);
                });
            case S_QM:
                return new EvalRule((self, env, state) -> {
                    state.push();
                    if (base.eval(env, state)) self.captures.putAll(base.captures);
                    else state.pop();
                    return true;
                });
            case S_AD:
                return new EvalRule((self, env, state) -> {
                    for (int i = 0; /* no condition */ ; ++i) {
                        state.push();
                        final boolean r = base.eval(env, state);
                        if (!r) {
                            state.pop();
                            return i != 0;
                        }
                        if (!base.captures.isEmpty()) {
                            self.captures.put(Integer.toString(i), new HashMap<>(base.captures));
                        }
                    }
                });
            case S_ST:
                return new EvalRule((self, env, state) -> {
                    for (int i = 0; /* no condition */ ; ++i) {
                        state.push();
                        if (!base.eval(env, state)) {
                            state.pop();
                            break;
                        }
                        if (!base.captures.isEmpty()) {
                            self.captures.put(Integer.toString(i), new HashMap<>(base.captures));
                        }
                    }
                    return true;
                });
            default:
                throw new RuntimeException("Unknown unary operator " + n.op);
        }
    }

    public EvalRule visitRefRule(final RefRule n) {
        final String txt = n.node.getText();
        return new EvalRule((self, env, state) -> {
            return env.get(txt).getRule().eval(self, env, state);
        });
    }

    public EvalRule visitKaryRule(final KaryRule n) {
        final EvalRule[] rules = n.rules.stream().map(this::visit).toArray(EvalRule[]::new);
        switch (n.type) {
            case SEQ:
                return new EvalRule((self, env, state) -> {
                    final HashMap<String, Object> merger = new HashMap<>();
                    for (int i = 0; i < rules.length; ++i) {
                        final EvalRule r = rules[i];
                        if (!r.eval(env, state)) return false;
                        merger.putAll(r.captures);
                    }
                    self.captures.putAll(merger);
                    return true;
                });
            case SWITCH:
                return new EvalRule((self, env, state) -> {
                    for (int i = 0; i < rules.length; ++i) {
                        final EvalRule r = rules[i];
                        state.push();
                        if (r.eval(env, state)) {
                            self.captures.putAll(r.captures);
                            return true;
                        }
                        state.pop();
                    }
                    return false;
                });
            default:
                throw new RuntimeException("Unknown rule block " + n.type);
        }
    }

    public EvalRule visitCaptureRule(final CaptureRule n) {
        final EvalRule rule = visit(n.rule);
        final String dest = n.dest.getText();
        return new EvalRule((self, env, state) -> {
            final int src = state.getIndex();
            final boolean ret = rule.eval(env, state);

            final Object[] capture = ret ? state.copyFromIndex(src) : new Object[0];
            if (rule.captures.isEmpty()) {
                self.captures.put(dest, capture);
            } else {
                final Map<String, Object> m = new HashMap<>();
                for (int i = 0; i < capture.length; ++i) {
                    m.put(Integer.toString(i), capture[i]);
                }
                self.captures.putAll(rule.captures);
                self.captures.put(dest, m);
                // System.out.println(" -- DEBUG" + self.captures);
            }
            return ret;
        });
    }

    public EvalRule visitBinaryRule(final BinaryRule n) {
        switch (n.op.type) {
            case S_MN: {               
                final Comparable a = (Comparable) ((ValueNode) n.rule1).toObject();
                final Comparable b = (Comparable) ((ValueNode) n.rule2).toObject();

                return new EvalRule((self, env, state) -> {
                    return state.testRange(a, b);
                });
            }
            default:
                throw new RuntimeException("Unknown binary operator " + n.op);
        }
    }
}