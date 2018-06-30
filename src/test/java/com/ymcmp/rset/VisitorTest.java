/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.IOException;
import java.io.StringReader;

import com.ymcmp.rset.Type;
import com.ymcmp.rset.tree.ValueNode;
import com.ymcmp.rset.tree.BinaryRule;

import com.ymcmp.lexparse.Token;
import com.ymcmp.lexparse.tree.Visitor;
import com.ymcmp.lexparse.tree.ParseTree;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;

public class VisitorTest {

    private ParseTree tree;

    @Before
    public void initializeTree() {
        tree = new BinaryRule(
                new Token<>(Type.S_AD, "+"),
                new ValueNode(new Token<>(Type.L_IDENT, "a")),
                new ValueNode(new Token<>(Type.L_IDENT, "b")));
    }

    @Test(expected = RuntimeException.class)
    public void noVisitorImplByDefaultThrowsRTE() {
        final Visitor<?> vis = new Visitor<Object>() {
            //
        };
        vis.visit(tree);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void exceptionsInVisitorImplAreCarriedUpwards() {
        final Visitor<?> vis = new Visitor<Object>() {

            public Object visitBinaryRule(BinaryRule rule) {
                // Child nodes are 0-indexed, the following code definitely triggers IndexOutOfBoundsException
                return visit(rule.getChild(rule.getChildCount()));
            }
        };
        vis.visit(tree);
    }

    @Test
    public void visitChildrenVisitsInOrder() {
        final int[] counter = new int[1];
        final Visitor<?> vis = new Visitor<String>() {

            public String visitBinaryRule(BinaryRule rule) {
                return visitChildren(rule);
            }

            public String visitValueNode(ValueNode node) {
                ++counter[0];
                return node.getText();
            }
        };

        // "a" is visited first, then "b" is
        // visitChildren returns the last visit result
        assertEquals("b", vis.visit(tree));
        assertEquals(2, counter[0]);
    }
}