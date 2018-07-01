/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.util.List;

import com.ymcmp.rset.tree.*;

import com.ymcmp.lexparse.Lexer;
import com.ymcmp.lexparse.Token;

import com.ymcmp.lexparse.tree.ParseTree;

public class RsetParser extends BaseParser<RulesetGroup> {

    private Lexer<Type> lexer;
    private Token<Type> buf;

    private RuleParser ruleParser = new RuleParser(this);
    private ActionParser actionParser = new ActionParser(this);

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

                final ParseTree rule = consumeRule(ruleParser::parse,
                        "Expected rule clause after new rule '" + name.getText() + "'");

                final RulesetNode rset = new RulesetNode(rulesetType, name, rule);

                final Token<Type> b = getToken();
                if (b != null && b.type == Type.S_LB) {
                    rset.expr = actionParser.parse();
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

    public RulesetGroup parseRulesets() {
        final List<RulesetNode> list = consumeRules(Type.S_CM, this::parseRuleset);
        if (list == null) return null;
        final int k = list.size() - 1;
        if (list.get(k) == null) list.remove(k);
        return new RulesetGroup(list);
    }
}