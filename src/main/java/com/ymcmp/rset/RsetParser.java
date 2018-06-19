package com.ymcmp.rset;

import java.util.List;
import java.util.ArrayList;

import com.ymcmp.rset.tree.*;

import com.ymcmp.lexparse.Lexer;
import com.ymcmp.lexparse.Token;
import com.ymcmp.lexparse.Parser;
import com.ymcmp.lexparse.IllegalParseException;

import com.ymcmp.lexparse.tree.ParseTree;

public class RsetParser implements Parser<Type, RulesetGroup> {

    private Lexer<Type> lexer;
    private Token<Type> buf;

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
                case S_MN: {
                    final Token<Type> inner = getToken();
                    if (inner != null && inner.type.isNumeric()) {
                        return new ValueNode(new Token<>(inner.type, '-' + inner.text));
                    }
                    throw new IllegalParseException("Expected numerical value after negative sign, found " + inner);
                }
                case S_MD: {
                    final Token<Type> inner = getToken();
                    if (inner != null && inner.type == Type.L_IDENT) {
                        return new ValueNode(new Token<>(Type.L_CHARS, '%' + inner.text));
                    }
                    throw new IllegalParseException("Expected identifier (or string) after '%', found " + inner);
                }
                case L_IDENT:
                case L_INT:
                case L_REAL:
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
                    final List<ParseTree> rules = consumeRules(this::parseRuleClause, Type.S_CM);
                    consumeToken(Type.S_RP, "Unclosed clause, missing ')'");

                    if (rules.isEmpty()) throw new IllegalParseException("Expected rule clauses in ( )");
                    if (rules.size() == 1) return rules.get(0);

                    final int k = rules.size() - 1;
                    if (rules.get(k) == null) rules.remove(k);
                    return new KaryRule(KaryRule.Type.GROUP, rules);
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

        if (rules == null) return null;
        if (rules.size() == 1) return rules.get(0);
        if (rules.get(rules.size() - 1) == null) {
            throw new IllegalParseException("Incomplete '|' clause, missing rhs");
        }
        return new KaryRule(KaryRule.Type.SWITCH, rules);
    }

    public ParseTree parseRuleClause() {
        return parseRuleSwitch();
    }

    public RulesetNode parseRuleset() {
        final Token<Type> t = getToken();
        if (t != null && t.type == Type.L_IDENT) {
            RulesetNode.Type rulesetType = null;
            switch (t.text) {
                default:
                    ungetToken(t);
                    break;
                case "rule":        // generates public test(Map)Z, act(Map)Object, rule(Object[])Object
                    rulesetType = RulesetNode.Type.RULE;
                    break;
                case "subrule":     // generates public test(Map)Z, act(Map)Object
                    rulesetType = RulesetNode.Type.SUBRULE;
                    break;
                case "fragment":    // inlines test, will cause error if being mutually referenced
                    rulesetType = RulesetNode.Type.FRAGMENT;
                    break;
            }

            if (rulesetType != null) {
                final ValueNode name = consumeRule(this::parseValue, "Missing name for ruleset");

                consumeToken(Type.S_EQ, "Expected '=' in ruleset '" + name.getText() + "' before rule");

                final ParseTree rule = consumeRule(this::parseRuleClause,
                        "Expected rule clause after new rule '" + name.getText() + "'");

                final RulesetNode rset = new RulesetNode(rulesetType, name, rule);

                final Token<Type> b = getToken();
                if (b != null && b.type == Type.S_LB) {
                    rset.expr = parseExpression();
                    consumeToken(Type.S_RB, "Unclosed Ruleset expression, missing '}'");
                    if (rulesetType == RulesetNode.Type.FRAGMENT && rset.expr != null) {
                        // fragments cannot have non-empty actions, warning
                        System.err.println("Warning: " + name.getText() + " is fragment type but contains non-empty action block");
                    }
                } else {
                    ungetToken(b);
                }
                return rset;
            }
        }
        return null;
    }

    public ParseTree parseExprValue() {
        final ValueNode o = parseValue();
        if (o != null) return o;

        final Token<Type> t = getToken();
        if (t != null) {
            switch (t.type) {
                case S_QM:
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
            throw new IllegalParseException("Incomplete ':' expression, missing rhs");
        }
        return new KaryRule(KaryRule.Type.SUBSCRIPT, substs);
    }

    private static final List<Type> TOKS_PARSE_MUL = new ArrayList<Type>() {{
        add(Type.S_ST);
        add(Type.S_DV);
        add(Type.S_MD);
    }};

    private static final List<Type> TOKS_PARSE_ADD = new ArrayList<Type>() {{
        add(Type.S_AD);
        add(Type.S_MN);
    }};

    public ParseTree parseMath() {
        return consumeRules(BinaryRule::new, () ->
                consumeRules(BinaryRule::new, this::parseSubscript,
                        TOKS_PARSE_MUL),
                TOKS_PARSE_ADD);
    }

    public ParseTree parseJoin() {
        final List<ParseTree> elms = consumeRules(this::parseMath, Type.S_TD);
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
            throw new IllegalParseException("Incomplete '&' expression, missing rhs");
        }
        return new KaryRule(KaryRule.Type.AND, elms);
    }

    public ParseTree parseExprOr() {
        final List<ParseTree> elms = consumeRules(this::parseExprAnd, Type.S_OR);

        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);
        if (elms.get(elms.size() - 1) == null) {
            throw new IllegalParseException("Incomplete '|' expression, missing rhs");
        }
        return new KaryRule(KaryRule.Type.OR, elms);
    }

    public ParseTree parseExprAssign() {
        final List<ParseTree> elms = consumeRules(this::parseExprOr, Type.S_EQ);

        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);
        if (elms.get(elms.size() - 1) == null) {
            throw new IllegalParseException("Incomplete '=' expression, missing rhs");
        }
        return new KaryRule(KaryRule.Type.ASSIGN, elms);
    }

    public ParseTree parseExprIgnore() {
        final List<ParseTree> elms = consumeRules(this::parseExprAssign, Type.S_EX);

        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);
        if (elms.get(elms.size() - 1) == null) {
            throw new IllegalParseException("Incomplete '!' expression, missing rhs");
        }
        return new KaryRule(KaryRule.Type.IGNORE, elms);
    }

    public ParseTree parseExpression() {
        return parseExprIgnore();
    }

    public RulesetGroup parseRulesets() {
        final List<RulesetNode> list = consumeRules(this::parseRuleset, Type.S_CM);
        if (list == null) return null;
        final int k = list.size() - 1;
        if (list.get(k) == null) list.remove(k);
        return new RulesetGroup(list);
    }
}