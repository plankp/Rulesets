/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import com.ymcmp.rset.tree.*;

import com.ymcmp.lexparse.Lexer;
import com.ymcmp.lexparse.Token;

import com.ymcmp.lexparse.tree.ParseTree;

/* package */ class RuleParser extends BaseParser<ParseTree> {

    private final BaseParser<?> outer;

    public RuleParser(BaseParser<?> outer) {
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
        return termPreservingSequence(KaryRule.Type.SWITCH, "'|'", Type.S_OR, () ->
                termNormalizingSequence(KaryRule.Type.SEQ, null, this::parseInnerLoop));
    }

    private ParseTree ruleAtomicHelper() {
        final Token<Type> t = getToken();
        if (t != null) {
            switch (t.type) {
                case S_ST:
                case S_SM:
                    return new ValueNode(t);
                case S_TD:
                    return new UnaryRule(t, parseRuleAtomic());
                case S_AM:
                    return new RefRule(parseValue());
                case S_EX:
                    return new UnaryRule(t, parseValue());
                case S_LS: {
                    final ParseTree rule = consumeRule(this::parse,
                            "Expecting rule to match against destructed list");
                    consumeToken(Type.S_RS, "Unclosed list destruction, missing ']'");
                    return new UnaryRule(t, rule);
                }
                case S_LP: {
                    final ParseTree rule = termNormalizingSequence(KaryRule.Type.GROUP, Type.S_CM, this::parse);
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
}