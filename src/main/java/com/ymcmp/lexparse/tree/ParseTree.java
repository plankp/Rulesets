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
            return (T) m.invoke(vis, this);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            return vis.visitMethodNotFound(this);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }
}