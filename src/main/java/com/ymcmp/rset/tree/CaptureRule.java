/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

import java.util.Map;
import java.util.HashMap;

import com.ymcmp.rset.Type;

import com.ymcmp.lexparse.Token;
import com.ymcmp.lexparse.tree.ParseTree;

public final class CaptureRule extends ParseTree {

    public final ParseTree rule;
    public final ValueNode dest;

    public CaptureRule(ValueNode tok, ParseTree rule) {
        this.dest = tok;
        this.rule = rule;
    }

    @Override
    public ParseTree getChild(int node) {
        if (node == 0) return dest;
        if (node == 1) return rule;
        throw new IndexOutOfBoundsException(node);
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public String getText() {
        return '(' + "set! " + dest.getText() + ' ' + rule.getText() + ')';
    }
}