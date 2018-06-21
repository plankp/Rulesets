/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

import com.ymcmp.lexparse.tree.ParseTree;

public final class RulesetNode extends ParseTree {

    public enum Type {
        RULE, SUBRULE, FRAGMENT;
    }

    public final Type type;
    public final ValueNode name;
    public final ParseTree rule;

    public ParseTree expr;

    public RulesetNode(Type t, ValueNode name, ParseTree rule) {
        this.type = t;
        this.name = name;
        this.rule = rule;
    }

    @Override
    public ParseTree getChild(int node) {
        if (node == 0) return rule;
        if (node == 1) return expr;
        throw new IndexOutOfBoundsException(node);
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public String getText() {
        return '(' + type.toString() + ' ' + name.getText()
            + ' ' + rule.getText() + ' ' + expr.getText() + ')';
    }
}