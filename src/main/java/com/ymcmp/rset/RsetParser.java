/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.util.List;
import java.util.ArrayList;

import java.util.function.Supplier;

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

    private ParseTree termNormalizingSequence(KaryRule.Type combineType, Type sep, Supplier<? extends ParseTree> rule) {
        final List<ParseTree> elms = consumeRules(rule, sep);
        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);

        if (sep != null) {
            // Only with non-null separators can null terms be a thing
            final int k = elms.size() - 1;
            if (elms.get(k) == null) elms.remove(k);
        }
        return new KaryRule(combineType, elms);
    }

    private ParseTree termPreservingSequence(KaryRule.Type combineType, String exprName, Type sep, Supplier<? extends ParseTree> rule) {
        final List<ParseTree> elms = consumeRules(rule, sep);
        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);

        if (elms.get(elms.size() - 1) == null) {
            throw new IllegalParseException("Incomplete " + exprName + " expression, missing rhs");
        }
        return new KaryRule(combineType, elms);
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
                    throw new IllegalParseException("Expected numerical value after negative sign", inner);
                }
                case S_MD: {
                    final Token<Type> inner = getToken();
                    if (inner != null) {
                        switch (inner.type) {
                            case L_IDENT:
                            case L_INT:
                            case L_REAL:
                                return new ValueNode(new Token<>(Type.L_CHARS, '%' + inner.text));
                        }
                    }
                    throw new IllegalParseException("Expected constant after '%'", inner);
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
                case S_LS: {
                    final ParseTree rule = consumeRule(this::parseRuleClause,
                            "Expecting rule to match against destructed list");
                    consumeToken(Type.S_RS, "Unclosed list destruction, missing ']'");
                    return new UnaryRule(t, rule);
                }
                case S_LP: {
                    final ParseTree rule = termNormalizingSequence(KaryRule.Type.GROUP, Type.S_CM, this::parseRuleClause);
                    consumeToken(Type.S_RP, "Unclosed clause, missing ')'");
                    if (rule == null) {
                        // Synthesize a null token, user wants to test for value NULL
                        return new ValueNode(new Token<>(Type.L_NULL, "()"));
                    }
                    return rule;
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

    public ParseTree parseRuleClause() {
        return termPreservingSequence(KaryRule.Type.SWITCH, "'|'", Type.S_OR, () ->
                termNormalizingSequence(KaryRule.Type.SEQ, null, this::parseInnerLoop));
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

    private static final List<Type> TOKS_PARSE_MUL = new ArrayList<Type>() {{
        add(Type.S_ST);
        add(Type.S_DV);
        add(Type.S_MD);
    }};

    private static final List<Type> TOKS_PARSE_ADD = new ArrayList<Type>() {{
        add(Type.S_AD);
        add(Type.S_MN);
    }};

    private BinaryRule delegateBinaryRuleCtor(Token<Type> tok, ParseTree rule1, ParseTree rule2) {
        // rule2 has a chance of being null
        if (rule2 == null) {
            throw new IllegalParseException("Operator " + tok.text + " expects two terms yet right side value is missing");
        }
        return new BinaryRule(tok, rule1, rule2);
    }

    public ParseTree parseMath() {
        return consumeRules(this::delegateBinaryRuleCtor, () ->
                consumeRules(this::delegateBinaryRuleCtor, () ->
                        termPreservingSequence(KaryRule.Type.SUBSCRIPT, "':'", Type.S_CO, this::parseExprValue),
                        TOKS_PARSE_MUL),
                TOKS_PARSE_ADD);
    }

    public ParseTree parseLoop() {
        final ParseTree tree =
                termNormalizingSequence(KaryRule.Type.CALL, null, () ->
                    termNormalizingSequence(KaryRule.Type.ARRAY, Type.S_CM, () ->
                        termNormalizingSequence(KaryRule.Type.JOIN, Type.S_TD, this::parseMath)));
        if (tree != null) {
            final Token<Type> tok = getToken();
            if (tok != null && tok.type == Type.S_LB) {
                final ParseTree inner = parseExpression();
                consumeToken(Type.S_RB, "Unclosed loop block, missing '}'");
                if (inner != null) return new BinaryRule(tok, tree, inner);
            } else {
                ungetToken(tok);
            }
        }
        return tree;
    }

    public ParseTree parseExpression() {
        return termPreservingSequence(KaryRule.Type.IGNORE, "'!'", Type.S_EX, () ->
                termPreservingSequence(KaryRule.Type.ASSIGN, "'='", Type.S_EQ, () ->
                        termPreservingSequence(KaryRule.Type.OR, "'|'", Type.S_OR, () ->
                        termPreservingSequence(KaryRule.Type.AND, "'&'", Type.S_AM, this::parseLoop))));
    }

    public RulesetGroup parseRulesets() {
        final List<RulesetNode> list = consumeRules(this::parseRuleset, Type.S_CM);
        if (list == null) return null;
        final int k = list.size() - 1;
        if (list.get(k) == null) list.remove(k);
        return new RulesetGroup(list);
    }
}