/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.lexparse.tree;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public abstract class ParseTree {

    protected ParseTree() {
        //
    }

    public abstract ParseTree getChild(int node);

    public abstract int getChildCount();

    public abstract String getText();

    public <T> T accept(final Visitor<T> vis) {
        // look for method T visitClassName (ClassName)
        final String synthName = "visit" + this.getClass().getSimpleName();
        try {
            final Method m = vis.getClass().getDeclaredMethod(synthName, this.getClass());
            m.setAccessible(true); // hacks!
            return (T) m.invoke(vis, this);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            return vis.visitMethodNotFound(this);
        } catch (InvocationTargetException ex) {
            final Throwable t = ex.getTargetException();
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            throw new RuntimeException(ex);
        }
    }
}