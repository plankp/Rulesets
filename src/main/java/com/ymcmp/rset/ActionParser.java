/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.util.Set;
import java.util.HashSet;

import com.ymcmp.rset.tree.*;

import com.ymcmp.lexparse.Lexer;
import com.ymcmp.lexparse.Token;
import com.ymcmp.lexparse.IllegalParseException;

import com.ymcmp.lexparse.tree.ParseTree;

/* package */ class ActionParser extends BaseParser<ParseTree> {

    private final BaseParser<?> outer;

    public ActionParser(BaseParser<?> outer) {
        this.outer = outer;
    }

    @Override
    public Lexer<Type> getLexer() {
        return outer.getLexer();
    }

    @Override
    public Token<Type> getToken() {
        return outer.getToken();
    }

    @Override
    public void ungetToken(final Token<Type> tok) {
        outer.ungetToken(tok);
    }

    @Override
    public ParseTree parse() {
        return termPreservingSequence(KaryRule.Type.IGNORE, "';'", Type.S_SM, () ->
                termPreservingSequence(KaryRule.Type.ASSIGN, "'='", Type.S_EQ, () ->
                        termPreservingSequence(KaryRule.Type.OR, "'|'", Type.S_OR, () ->
                        termPreservingSequence(KaryRule.Type.AND, "'&'", Type.S_AM, this::parseLoop))));
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
                    final ParseTree r = parse();
                    consumeToken(Type.S_RP, "Unclosed expression, missing ')'");
                    return r;
                }
                default:
                    ungetToken(t);
            }
        }
        return null;
    }

    private static final Set<Type> TOKS_PARSE_MUL = new HashSet<Type>() {{
        add(Type.S_ST);
        add(Type.S_DV);
        add(Type.S_MD);
    }};

    private static final Set<Type> TOKS_PARSE_ADD = new HashSet<Type>() {{
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
        return consumeRules(this::delegateBinaryRuleCtor, TOKS_PARSE_ADD, () ->
                consumeRules(this::delegateBinaryRuleCtor, TOKS_PARSE_MUL, () ->
                        termPreservingSequence(KaryRule.Type.SUBSCRIPT, "':'", Type.S_CO, this::parseExprValue)));
    }

    public ParseTree parseLoop() {
        final ParseTree tree =
                termNormalizingSequence(KaryRule.Type.CALL, null, () ->
                    termNormalizingSequence(KaryRule.Type.ARRAY, Type.S_CM, () ->
                        termNormalizingSequence(KaryRule.Type.JOIN, Type.S_TD, this::parseMath)));
        if (tree != null) {
            final Token<Type> tok = getToken();
            if (tok != null && tok.type == Type.S_LB) {
                final ParseTree inner = parse();
                consumeToken(Type.S_RB, "Unclosed loop block, missing '}'");
                if (inner != null) return new BinaryRule(tok, tree, inner);
            } else {
                ungetToken(tok);
            }
        }
        return tree;
    }
}