/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

import com.ymcmp.rset.Type;

import com.ymcmp.lexparse.Token;
import com.ymcmp.lexparse.tree.ParseTree;

public final class RefRule extends ParseTree {

    public final ValueNode node;

    public RefRule(ValueNode node) {
        this.node = node;
    }

    @Override
    public ParseTree getChild(int node) {
        throw new IndexOutOfBoundsException(node);
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String getText() {
        return '(' + "get " + node.getText() + ')';
    }
}