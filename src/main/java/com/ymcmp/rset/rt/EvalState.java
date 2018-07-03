/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.rt;

import java.lang.reflect.Method;

import java.util.Stack;
import java.util.Arrays;
import java.util.Objects;
import java.util.Collection;
import java.util.EmptyStackException;

import static com.ymcmp.rset.lib.Mathlib.compare;

public class EvalState {

    protected final Stack<Integer> indexes = new Stack<>();

    protected Object[] data;

    private boolean negateFlag;

    public void setData(Object... data) {
        this.data = data;
    }

    public void setNegateFlag(boolean flag) {
        this.negateFlag = flag;
    }

    public boolean getNegateFlag() {
        return this.negateFlag;
    }

    public void reset() {
        data = new Object[0];
        indexes.clear();
        indexes.push(0);
        negateFlag = false;
    }

    public Object next() {
        try {
            final int i = indexes.pop();
            indexes.push(i + 1);
            return data[i];
        } catch (IndexOutOfBoundsException ex) {
            prev();
            return Epsilon.INSTANCE;
        }
    }

    public void prev() {
        final int i = indexes.pop();
        indexes.push(i - 1);
    }

    public void unsave() {
        indexes.pop();
    }

    public void save() {
        indexes.push(indexes.peek());
    }

    public EvalState destructArray() {
        final Object k = next();
        if (k == null) return null;

        Object[] destruct = null;
        if (k.getClass().isArray()) {
            destruct = (Object[]) k;
        } else if (k instanceof Collection) {
            destruct = ((Collection<?>) k).toArray();
        } else {
            return null;
        }

        final EvalState destructedState = new EvalState();
        destructedState.reset();
        destructedState.setData(destruct);
        destructedState.setNegateFlag(this.negateFlag);
        return destructedState;
    }

    private boolean processNegate(final boolean b) {
        return negateFlag ? !b : b;
    }

    private boolean condAdd(final boolean test, final Object k, final Collection<Object> col) {
        if (test) {
            if (col != null) col.add(k);
            return true;
        }
        return false;
    }

    public boolean testInheritance(final Class cl, final boolean from, final Collection<Object> col) {
        final Object k = next();

        // null is not a type, without negateFlag, it will always be false
        if (k == null) return condAdd(negateFlag, null, col);

        final Class ck = k.getClass();
        return condAdd(processNegate(from ? cl.isAssignableFrom(ck) : ck.isAssignableFrom(cl)), k, col);
    }

    private static boolean classHasField(final Class<?> cl, final String selector) {
        // Arrays have length as attribute but
        // it is actually not a field to the class's concern
        if (cl.isArray()) {
            return "length".equals(selector);
        } else {
            try {
                // Is it even possible for getField to return null?
                return cl.getField(selector) != null;
            } catch (NoSuchFieldException ex) {
                // class does not have field that is public
            }
        }
        return false;
    }

    private static boolean classHasMethod(final Class<?> cl, final String selector) {
        // Not using cl.getMethod(selector) because only care if method exists,
        // whereas getMethod expects us to know the parameter types
        for (final Method m : cl.getMethods()) {
            if (m.getName().equals(selector)) return true;
        }
        return false;
    }

    public boolean hasFieldOrMethod(final String selector, final Collection<Object> col) {
        final Object k = next();

        // null can not contain any field or method
        // if negateFlag is on, this must return true
        if (k == null) return condAdd(negateFlag, null, col);

        final Class<?> cl = k.getClass();
        return condAdd(processNegate(classHasField(cl, selector)) || processNegate(classHasMethod(cl, selector)), k, col);
    }

    public boolean testEquality(final Object obj, final Collection<Object> col) {
        final Object k = next();
        return condAdd(!Epsilon.INSTANCE.equals(k) && processNegate(Objects.equals(obj, k)), k, col);
    }

    public boolean testRange(final Comparable a, final Comparable b, final Collection<Object> col) {
        final Object k = next();
        try {
            // To be in range, either:
            //    a <= k and b >= k
            // or b <= k and a >= k
            // Which becomes
            // let u = a - k, v = b - k
            //    u <= 0 and v >= 0
            // or v <= 0 and u >= 0

            final Comparable<?> ck = (Comparable<?>) k;
            final int u = compare(a, ck);
            final int v = compare(b, ck);
            return condAdd(processNegate(u <= 0 && v >= 0 || v <= 0 && u >= 0), k, col);
        } catch (ClassCastException | NullPointerException ex) {
            // current slot value does not belong in set,
            // which satisfies *not* being in range.
            // return true if negate flag is on
            return condAdd(!Epsilon.INSTANCE.equals(k) && negateFlag, k, col);
        }
    }

    public boolean testSlotOccupied(Collection<Object> col) {
        return condAdd(!negateFlag, next(), col);
    }

    public boolean testEnd(Collection<Object> col) {
        final Object k = next();
        return condAdd(processNegate(Epsilon.INSTANCE.equals(k)), k, col);
    }
}