package com.ymcmp.rset.tree;

import com.ymcmp.lexparse.tree.ParseTree;

public final class RulesetNode extends ParseTree {

    public final ValueNode name;
    public final ParseTree rule;
    public ParseTree expr;

    public RulesetNode(ValueNode name, ParseTree rule) {
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
        return '(' + "rule " + name.getText()
            + ' ' + rule.getText() + ' ' + expr.getText() + ')';
    }
}