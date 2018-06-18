package com.ymcmp.rset.tree;

import java.util.List;

import java.util.stream.Collectors;

import com.ymcmp.rset.Type;

import com.ymcmp.lexparse.Token;
import com.ymcmp.lexparse.tree.ParseTree;

public final class KaryRule extends ParseTree {

    public enum Type {
        SEQ, GROUP, SWITCH,
        SUBSCRIPT, JOIN, ARRAY,
        CALL, AND, OR, ASSIGN,
        IGNORE;
    }

    public final Type type;
    public final List<ParseTree> rules;

    public KaryRule(Type type, List<ParseTree> rules) {
        this.type = type;
        this.rules = rules;
    }

    @Override
    public ParseTree getChild(int node) {
        return rules.get(node);
    }

    @Override
    public int getChildCount() {
        return rules.size();
    }

    @Override
    public String getText() {
        return '(' + type.toString() + ' ' + rules.stream()
                .map(ParseTree::getText).collect(Collectors.joining(" ")) + ')';
    }
}