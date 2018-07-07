/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

import java.util.List;
import java.util.Collections;

import com.ymcmp.rset.Type;

import com.ymcmp.lexparse.Token;
import com.ymcmp.lexparse.tree.ParseTree;

public final class RefRule extends ParseTree {

    public final ValueNode node;

    public List<ParseTree> subst;

    public RefRule(ValueNode node) {
        this.node = node;
    }

    @Override
    public ParseTree getChild(int node) {
        return (subst == null ? ((List<ParseTree>) Collections.EMPTY_LIST) : subst).get(node);
    }

    @Override
    public int getChildCount() {
        return subst == null ? 0 : subst.size();
    }

    @Override
    public String getText() {
        if (subst == null) {
            return '(' + "get " + node.getText() + ')';
        } else {
            return '(' + "subst " + node.getText() + ' ' + subst + ')';
        }
    }
}