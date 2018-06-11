package com.ymcmp.rset.tree;

import com.ymcmp.rset.Type;

import com.ymcmp.engine.Token;
import com.ymcmp.engine.tree.ParseTree;

public final class BinaryRule extends ParseTree {

    public final ParseTree rule1, rule2;
    public final Token<Type> op;

    public BinaryRule(Token<Type> tok, ParseTree rule1, ParseTree rule2) {
        this.op = tok;
        this.rule1 = rule1;
        this.rule2 = rule2;
    }

    @Override
    public ParseTree getChild(int node) {
        if (node == 0) return rule1;
        if (node == 1) return rule2;
        throw new IndexOutOfBoundsException(node);
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public String getText() {
        return '(' + op.text + ' ' + rule1.getText() + ' ' + rule2.getText() + ')';
    }
}