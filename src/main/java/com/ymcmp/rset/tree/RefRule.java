package com.ymcmp.rset.tree;

import com.ymcmp.rset.Type;

import com.ymcmp.engine.Token;
import com.ymcmp.engine.tree.ParseTree;

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