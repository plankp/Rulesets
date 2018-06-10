package com.ymcmp.rset;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;

import java.util.function.Function;
import java.util.function.Supplier;

import com.ymcmp.rset.lib.Stdlib;

public class Parser {

    private Lexer lexer;
    private Token buf;

    public Parser(final Lexer lexer) {
        this.lexer = lexer;
    }

    private Token getToken() {
        if (buf == null) return lexer.nextToken();
        final Token t = buf;
        buf = null;
        return t;
    }

    private void ungetToken(final Token tok) {
        if (buf != null) throw new RuntimeException("Buffer is filled");
        if (tok == null) return;
        buf = tok;
    }

    public Token consume(Token.Type type, String messageWhenUnmatched) {
        final Token token = getToken();
        if (token == null || token.type != type) {
            throw new RuntimeException(messageWhenUnmatched);
        }
        return token;
    }

    public <T> List<T> consumeWithDelim(Supplier<? extends T> rule, Token.Type delim) {
        final T head = rule.get();
        if (head == null) return null;

        final List<T> elms = new ArrayList<>();
        elms.add(head);

        while (true) {
            if (delim != null) {
                final Token t = getToken();
                if (t == null || t.type != delim) {
                    ungetToken(t);
                    break;
                }
            }
            final T el = rule.get();
            if (el == null) {
                if (delim != null) elms.add(null);
                break;
            }
            elms.add(el);
        }
        return elms;
    }

    public Object parseValue() {
        final Token t = getToken();
        if (t != null) {
            switch (t.type) {
                case L_IDENT:  return t.text.length() == 1 ? t.text.charAt(0) : t.text;
                case L_NUMBER: return Integer.parseInt(t.text);
                default:
                    ungetToken(t);
            }
        }
        return null;
    }

    private EvalRule ruleAtomicHelper() {
        final Token t = getToken();
        if (t != null) {
            switch (t.type) {
                case S_ST: return new EvalRule((self, env, state) -> state.testSlotOccupied());
                case S_EX: return new EvalRule((self, env, state) -> state.testEnd());
                case S_TD: {
                    final EvalRule subrule = parseRuleAtomic();
                    return new EvalRule((self, env, state) -> !subrule.eval(self, env, state));
                }
                case S_AM: {
                    final String ref = parseValue().toString();
                    return new EvalRule((self, env, state) -> env.get(ref).getRule().eval(self, env, state));
                }
                case S_LP: {
                    final EvalRule r = parseRuleClause();
                    consume(Token.Type.S_RP, "Unclosed clause, missing ')'");
                    return r;
                }
                default:
                    ungetToken(t);
            }
        }
        return null;
    }

    public EvalRule parseRuleAtomic() {
        final Object exp = parseValue();
        if (exp == null) return ruleAtomicHelper();

        // Check if there is a colon following it
        final Token t = getToken();
        if (t != null) {
            switch (t.type) {
                case S_CO: {
                    // Have to parse subsequent part
                    final EvalRule rule = parseRuleAtomic();
                    if (rule == null) {
                        throw new RuntimeException("Expected rule clause after '" + exp + "' capture");
                    }
                    return new EvalRule((self, env, state) -> {
                        final int src = state.getIndex();
                        final boolean ret = rule.eval(env, state);

                        final Object[] capture = ret ? state.copyFromIndex(src) : new Object[0];
                        if (rule.captures.isEmpty()) {
                            self.captures.put(exp.toString(), capture);
                        } else {
                            final Map<String, Object> m = new HashMap<>();
                            for (int i = 0; i < capture.length; ++i) {
                                m.put(Integer.toString(i), capture[i]);
                            }
                            self.captures.put(exp.toString(), m);
                        }
                        return ret;
                    });
                }
                case S_MN: {               
                    final Object b = parseValue();
                    if (b == null) throw new RuntimeException("Incomplete range declaration from '" + exp + "'");

                    return new EvalRule((self, env, state) -> state.testRange((Comparable) exp, (Comparable) b));
                }
                default:
                    ungetToken(t);
            }
        }

        // Otherwise, it is just a simple matching rule
        return new EvalRule((self, env, state) -> state.testEquality(exp));
    }

    public EvalRule parseInnerLoop() {
        final EvalRule base = parseRuleAtomic();
        if (base == null) return null;

        final Token t = getToken();
        if (t != null) {
            switch (t.type) {
            case S_QM:
                return new EvalRule((self, env, state) -> {
                    base.captures.clear();
                    state.push();
                    if (base.eval(env, state)) self.captures.putAll(base.captures);
                    else state.pop();
                    return true;
                });
            case S_AD:
                return new EvalRule((self, env, state) -> {
                    for (int i = 0; /* no condition */ ; ++i) {
                        base.captures.clear();
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
                        base.captures.clear();
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
                ungetToken(t);
            }
        }
        return base;
    }

    public EvalRule parseRuleSequence() {
        final List<EvalRule> rules = new ArrayList<>();
        EvalRule clause;
        while ((clause = parseInnerLoop()) != null) {
            rules.add(clause);
        }

        if (rules.isEmpty()) return null;
        if (rules.size() == 1) return rules.get(0);
        return new EvalRule((self, env, state) -> {
            final HashMap<String, Object> merger = new HashMap<>();
            for (int i = 0; i < rules.size(); ++i) {
                final EvalRule r = rules.get(i);
                if (!r.eval(env, state)) return false;
                merger.putAll(r.captures);
            }
            self.captures.putAll(merger);
            return true;
        });
    }

    public EvalRule parseRuleSwitch() {
        final List<EvalRule> rules = new ArrayList<>();
        EvalRule clause;
        while ((clause = parseRuleSequence()) != null) {
            rules.add(clause);

            final Token t = getToken();
            if (t != null && t.type == Token.Type.S_OR) continue;
            ungetToken(t);
            break;
        }

        if (rules.isEmpty()) return null;
        if (rules.size() == 1) return rules.get(0);
        return new EvalRule((self, env, state) -> {
            for (int i = 0; i < rules.size(); ++i) {
                final EvalRule r = rules.get(i);
                state.push();
                if (r.eval(env, state)) {
                    self.captures.putAll(r.captures);
                    return true;
                }
                state.pop();
            }
            return false;
        });
    }

    public EvalRule parseRuleClause() {
        return parseRuleSwitch();
    }

    public Ruleset parseRuleset() {
        final Token t = getToken();
        if (t != null) {
            if (t.type == Token.Type.L_IDENT && "rule".equals(t.text)) {
                Object name = parseValue();
                if (name == null) {
                    throw new RuntimeException("Missing name for ruleset");
                }

                consume(Token.Type.S_EQ, "Expected '=' in ruleset '" + name + "' before rule");

                final EvalRule rule = parseRuleClause();
                if (rule == null) {
                    throw new RuntimeException("Expected rule clause after new rule '" + name + "'");
                }

                final Ruleset rset = new Ruleset(name.toString(), rule);

                final Token b = getToken();
                if (b != null && b.type == Token.Type.S_LB) {
                    rset.setAction(parseExpression());
                    consume(Token.Type.S_RB, "Unclosed Ruleset expression, missing '}'");
                } else {
                    ungetToken(b);
                }
                return rset;
            }
            ungetToken(t);
        }
        return null;
    }

    public ExprAction parseExprValue() {
        final Object o = parseValue();
        if (o != null) return (env) -> o;

        final Token t = getToken();
        if (t != null) {
            switch (t.type) {
                case S_ST: {
                    final ExprAction ref = parseExprValue();
                    return (env) -> env.get(ref.apply(env).toString());
                }
                case S_LP: {
                    final ExprAction r = parseExpression();
                    consume(Token.Type.S_RP, "Unclosed expression, missing ')'");
                    return r;
                }
                default:
                    ungetToken(t);
            }
        }
        return null;
    }

    public ExprAction parseSubscript() {
        final List<ExprAction> substs = consumeWithDelim(this::parseExprValue, Token.Type.S_CO);
        if (substs == null) return null;
        if (substs.size() == 1) return substs.get(0);

        if (substs.get(substs.size() - 1) == null) {
            throw new RuntimeException("Incomplete ':' expression, missing rhs");
        }
        return (env) -> substs.stream().map(e -> e.apply(env))
                .reduce(Stdlib::subscript)
                .orElse(null);
    }

    public ExprAction parseJoin() {
        final List<ExprAction> elms = consumeWithDelim(this::parseSubscript, Token.Type.S_TD);
        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);

        final int k = elms.size() - 1;
        if (elms.get(k) == null) elms.remove(k);
        return (env) -> Stdlib.concat(elms.stream().map(e -> e.apply(env))
                .toArray(Object[]::new));
    }

    public ExprAction parseArray() {
        final List<ExprAction> elms = consumeWithDelim(this::parseJoin, Token.Type.S_CM);
        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);

        final int k = elms.size() - 1;
        if (elms.get(k) == null) elms.remove(k);
        return (env) -> elms.stream().map(e -> e.apply(env)).toArray();
    }

    public ExprAction parseCall() {
        final List<ExprAction> elms = consumeWithDelim(this::parseArray, null);

        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);
        return (env) -> {
            final Object k = elms.get(0).apply(env);
            final Object[] args = new Object[elms.size() - 1];
            for (int i = 1; i < elms.size(); ++i) {
                args[i - 1] = elms.get(i).apply(env);
            }
            return ((Function<Object[], ?>) k).apply(args);
        };
    }

    public ExprAction parseExprAnd() {
        final List<ExprAction> elms = consumeWithDelim(this::parseCall, Token.Type.S_AM);

        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);
        if (elms.get(elms.size() - 1) == null) {
            throw new RuntimeException("Incomplete '&' expression, missing rhs");
        }
        return (env) -> {
            Object u = null;
            for (int i = 0; i < elms.size(); ++i) {
                u = elms.get(i).apply(env);
                if (!Stdlib.isTruthy(u)) return u;
            }
            return u;
        };
    }

    public ExprAction parseExprOr() {
        final List<ExprAction> elms = consumeWithDelim(this::parseExprAnd, Token.Type.S_OR);

        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);
        if (elms.get(elms.size() - 1) == null) {
            throw new RuntimeException("Incomplete '|' expression, missing rhs");
        }
        return (env) -> {
            Object u = null;
            for (int i = 0; i < elms.size(); ++i) {
                u = elms.get(i).apply(env);
                if (Stdlib.isTruthy(u)) return u;
            }
            return u;
        };
    }

    public ExprAction parseExprAssign() {
        final List<ExprAction> elms = consumeWithDelim(this::parseExprOr, Token.Type.S_EQ);

        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);
        if (elms.get(elms.size() - 1) == null) {
            throw new RuntimeException("Incomplete '=' expression, missing rhs");
        }
        return (env) -> {
            Object u = elms.get(0).apply(env);
            for (int i = 1; i < elms.size(); ++i) {
                final Object k = elms.get(i).apply(env);
// NOTE: This implementation does not mutate maps or arrays
                env.put(u.toString(), k);
                u = k;
            }
            return u;
        };
    }

    public ExprAction parseExpression() {
        return parseExprAssign();
    }

    public List<Ruleset> parseRulesets() {
        return consumeWithDelim(this::parseRuleset, Token.Type.S_CM);
    }
}