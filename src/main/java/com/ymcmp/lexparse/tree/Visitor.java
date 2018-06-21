/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.lexparse.tree;

public abstract class Visitor<T> {

    /*
     * Visitor operates through reflection
     * method must be:
     *   public T visit{Class} ({Class});
     *
     * For example a visit method for FooExpr:
     *   public T visitFooExpr(FooExpr node);
     *
     * If the specified method cannot be found,
     * the runtime will call visitMethodNotFound
     */

    public T visit(final ParseTree tree) {
        return tree.accept(this);
    }

    public T visitMethodNotFound(final ParseTree tree) {
        throw new RuntimeException("Visitor method for " + tree.getClass().getSimpleName() + " not found");
    }

    public T visitChildren(final ParseTree tree) {
        T t = null;
        for (int i = 0; i < tree.getChildCount(); ++i) {
            t = visit(tree.getChild(i));
        }
        return t;
    }
}