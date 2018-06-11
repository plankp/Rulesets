package com.ymcmp.rset.tree;

import com.ymcmp.rset.Ruleset;

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

    public Ruleset toRuleset() {
        final RuleVisitor rv = new RuleVisitor();
        final ActionVisitor av = new ActionVisitor();

        final Ruleset rset = new Ruleset(name.getText(), rv.visit(rule));
        if (expr != null) rset.setAction(av.visit(expr));
        return rset;
    }
}