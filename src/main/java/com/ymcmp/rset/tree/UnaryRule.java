/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

import com.ymcmp.rset.Type;

import com.ymcmp.lexparse.Token;
import com.ymcmp.lexparse.tree.ParseTree;

public final class UnaryRule extends ParseTree {

    public final ParseTree rule;
    public final Token<Type> op;

    public UnaryRule(Token<Type> tok, ParseTree rule) {
        this.op = tok;
        this.rule = rule;
    }

    @Override
    public ParseTree getChild(int node) {
        if (node == 0) return rule;
        throw new IndexOutOfBoundsException(node);
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String getText() {
        return '(' + op.text + ' ' + rule.getText() + ')';
    }
}