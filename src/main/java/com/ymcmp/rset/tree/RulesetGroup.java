package com.ymcmp.rset.tree;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import com.ymcmp.rset.Ruleset;

import com.ymcmp.lexparse.tree.ParseTree;

public final class RulesetGroup extends ParseTree {

    public List<RulesetNode> rsets;

    public RulesetGroup(List<RulesetNode> rsets) {
        this.rsets = rsets;
    }

    @Override
    public ParseTree getChild(int node) {
        return rsets.get(node);
    }

    @Override
    public int getChildCount() {
        return rsets.size();
    }

    @Override
    public String getText() {
        return '(' + "begin " + rsets.stream().map(ParseTree::getText)
                .collect(Collectors.joining(" ")) + ')';
    }

    public Stream<Ruleset> toRulesetStream() {
        return rsets.stream().map(RulesetNode::toRuleset);
    }
}