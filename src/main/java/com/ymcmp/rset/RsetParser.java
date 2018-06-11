package com.ymcmp.rset;

import java.util.List;
import java.util.ArrayList;

import com.ymcmp.rset.tree.*;

import com.ymcmp.engine.Lexer;
import com.ymcmp.engine.Token;
import com.ymcmp.engine.Parser;
import com.ymcmp.engine.tree.ParseTree;

public class RsetParser implements Parser<Type, RulesetGroup> {

    private Lexer<Type> lexer;
    private Token buf;

    public RsetParser(final Lexer<Type> lexer) {
        this.lexer = lexer;
    }

    @Override
    public Lexer<Type> getLexer() {
        return lexer;
    }

    @Override
    public Token<Type> getToken() {
        if (buf == null) return lexer.nextToken();
        final Token<Type> t = buf;
        buf = null;
        return t;
    }

    @Override
    public void ungetToken(final Token<Type> tok) {
        if (buf != null) throw new RuntimeException("Buffer is filled");
        if (tok == null) return;
        buf = tok;
    }

    @Override
    public RulesetGroup parse() {
        return parseRulesets();
    }

    public ValueNode parseValue() {
        final Token<Type> t = getToken();
        if (t != null) {
            switch (t.type) {
                case L_IDENT:
                case L_NUMBER:
                    return new ValueNode(t);
                default:
                    ungetToken(t);
            }
        }
        return null;
    }

    private ParseTree ruleAtomicHelper() {
        final Token<Type> t = getToken();
        if (t != null) {
            switch (t.type) {
                case S_ST:
                case S_EX:
                    return new ValueNode(t);
                case S_TD:
                    return new UnaryRule(t, parseRuleAtomic());
                case S_AM:
                    return new RefRule(parseValue());
                case S_LP: {
                    final ParseTree r = parseRuleClause();
                    consumeToken(Type.S_RP, "Unclosed clause, missing ')'");
                    return r;
                }
                default:
                    ungetToken(t);
            }
        }
        return null;
    }

    public ParseTree parseRuleAtomic() {
        final ValueNode exp = parseValue();
        if (exp == null) return ruleAtomicHelper();

        // Check if there is a colon following it
        final Token<Type> t = getToken();
        if (t != null) {
            switch (t.type) {
                case S_CO:
                    // Have to parse subsequent part
                    return new CaptureRule(exp, consumeRule(this::parseRuleAtomic,
                            "Expected rule clause after '" + exp.getText() + "' capture"));
                case S_MN:
                    return new BinaryRule(t, exp, consumeRule(this::parseValue,
                            "Incomplete range declaration from '" + exp.getText() + "'"));
                default:
                    ungetToken(t);
            }
        }
        return exp;
    }

    public ParseTree parseInnerLoop() {
        final ParseTree base = parseRuleAtomic();
        if (base == null) return null;

        final Token<Type> t = getToken();
        if (t != null) {
            switch (t.type) {
                case S_QM:
                case S_AD:
                case S_ST:
                    return new UnaryRule(t, base);
                default:
                    ungetToken(t);
            }
        }
        return base;
    }

    public ParseTree parseRuleSequence() {
        final List<ParseTree> rules = consumeRules(this::parseInnerLoop, null);

        if (rules == null) return null;
        if (rules.size() == 1) return rules.get(0);
        return new KaryRule(KaryRule.Type.SEQ, rules);
    }

    public ParseTree parseRuleSwitch() {
        final List<ParseTree> rules = consumeRules(this::parseRuleSequence, Type.S_OR);

        if (rules.isEmpty()) return null;
        if (rules.size() == 1) return rules.get(0);
        return new KaryRule(KaryRule.Type.SWITCH, rules);
    }

    public ParseTree parseRuleClause() {
        return parseRuleSwitch();
    }

    public RulesetNode parseRuleset() {
        final Token<Type> t = getToken();
        if (t != null) {
            if (t.type == Type.L_IDENT && "rule".equals(t.text)) {
                final ValueNode name = parseValue();
                if (name == null) {
                    throw new RuntimeException("Missing name for ruleset");
                }

                consumeToken(Type.S_EQ, "Expected '=' in ruleset '" + name.getText() + "' before rule");

                final ParseTree rule = consumeRule(this::parseRuleClause,
                        "Expected rule clause after new rule '" + name.getText() + "'");

                final RulesetNode rset = new RulesetNode(name, rule);

                final Token<Type> b = getToken();
                if (b != null && b.type == Type.S_LB) {
                    rset.expr = parseExpression();
                    consumeToken(Type.S_RB, "Unclosed Ruleset expression, missing '}'");
                } else {
                    ungetToken(b);
                }
                return rset;
            }
            ungetToken(t);
        }
        return null;
    }

    public ParseTree parseExprValue() {
        final ValueNode o = parseValue();
        if (o != null) return o;

        final Token<Type> t = getToken();
        if (t != null) {
            switch (t.type) {
                case S_ST:
                    return new UnaryRule(t, parseExprValue());
                case S_LP: {
                    final ParseTree r = parseExpression();
                    consumeToken(Type.S_RP, "Unclosed expression, missing ')'");
                    return r;
                }
                default:
                    ungetToken(t);
            }
        }
        return null;
    }

    public ParseTree parseSubscript() {
        final List<ParseTree> substs = consumeRules(this::parseExprValue, Type.S_CO);
        if (substs == null) return null;
        if (substs.size() == 1) return substs.get(0);

        if (substs.get(substs.size() - 1) == null) {
            throw new RuntimeException("Incomplete ':' expression, missing rhs");
        }
        return new KaryRule(KaryRule.Type.SUBSCRIPT, substs);
    }

    public ParseTree parseJoin() {
        final List<ParseTree> elms = consumeRules(this::parseSubscript, Type.S_TD);
        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);

        final int k = elms.size() - 1;
        if (elms.get(k) == null) elms.remove(k);
        return new KaryRule(KaryRule.Type.JOIN, elms);
    }

    public ParseTree parseArray() {
        final List<ParseTree> elms = consumeRules(this::parseJoin, Type.S_CM);
        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);

        final int k = elms.size() - 1;
        if (elms.get(k) == null) elms.remove(k);
        return new KaryRule(KaryRule.Type.ARRAY, elms);
    }

    public ParseTree parseCall() {
        final List<ParseTree> elms = consumeRules(this::parseArray, null);

        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);
        return new KaryRule(KaryRule.Type.CALL, elms);
    }

    public ParseTree parseExprAnd() {
        final List<ParseTree> elms = consumeRules(this::parseCall, Type.S_AM);

        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);
        if (elms.get(elms.size() - 1) == null) {
            throw new RuntimeException("Incomplete '&' expression, missing rhs");
        }
        return new KaryRule(KaryRule.Type.AND, elms);
    }

    public ParseTree parseExprOr() {
        final List<ParseTree> elms = consumeRules(this::parseExprAnd, Type.S_OR);

        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);
        if (elms.get(elms.size() - 1) == null) {
            throw new RuntimeException("Incomplete '|' expression, missing rhs");
        }
        return new KaryRule(KaryRule.Type.OR, elms);
    }

    public ParseTree parseExprAssign() {
        final List<ParseTree> elms = consumeRules(this::parseExprOr, Type.S_EQ);

        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);
        if (elms.get(elms.size() - 1) == null) {
            throw new RuntimeException("Incomplete '=' expression, missing rhs");
        }
        return new KaryRule(KaryRule.Type.ASSIGN, elms);
    }

    public ParseTree parseExpression() {
        return parseExprAssign();
    }

    public RulesetGroup parseRulesets() {
        return new RulesetGroup(consumeRules(this::parseRuleset, Type.S_CM));
    }
}