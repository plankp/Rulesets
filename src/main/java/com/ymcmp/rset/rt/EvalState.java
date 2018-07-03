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

    private int silencedPop() {
        try {
            return indexes.pop();
        } catch (EmptyStackException ex) {
            return 0;
        }
    }

    public int next() {
        final int i = silencedPop();
        indexes.push(i + 1);
        return i;
    }

    public int prev() {
        final int i = silencedPop();
        indexes.push(i - 1);
        return i;
    }

    public int getIndex() {
        try {
            return indexes.peek();
        } catch (EmptyStackException ex) {
            //
        }
        return 0;
    }

    public Object[] copyRange(int from, int to) {
        return Arrays.copyOfRange(data, from, to);
    }

    public void unsave() {
        indexes.pop();
    }

    public void save() {
        indexes.push(getIndex());
    }

    public EvalState destructArray() {
        try {
            final Object k = data[next()];
            Object[] destruct = null;
            if (k == null) return null;
            if (k.getClass().isArray()) {
                destruct = (Object[]) k;
            } else if (k instanceof Collection) {
                destruct = ((Collection<?>) k).toArray();
            } else {
                return null;
            }

            final EvalState destructedState = new EvalState();
            destructedState.setData(destruct);
            destructedState.setNegateFlag(this.negateFlag);
            return destructedState;
        } catch (IndexOutOfBoundsException ex) {
            prev();
        }
        return null;
    }

    private boolean processNegate(final boolean b) {
        return negateFlag ? !b : b;
    }

    public boolean testInheritance(final Class cl, final boolean from, final Collection<Object> col) {
        try {
            final Object k = data[next()];

            if (k == null) {
                // null is not a type, without negateFlag, it will always be false
                if (negateFlag) {
                   if (col != null) col.add(null);
                   return true;
                }
                return false;
            }

            final Class ck = k.getClass();
            final boolean r = from ? cl.isAssignableFrom(ck) : ck.isAssignableFrom(cl);
            if (negateFlag ? !r : r) {
                if (col != null) col.add(k);
                return true;
            }
        } catch (IndexOutOfBoundsException ex) {
            prev();
        }
        return false;
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
        try {
            final Object k = data[next()];
            if (k == null) {
                // null can not contain any field or method
                // if negateFlag is on, this must return true
                if (negateFlag) {
                    if (col != null) col.add(null);
                    return true;
                }
                return false;
            }

            final Class<?> cl = k.getClass();
            if (processNegate(classHasField(cl, selector)) || processNegate(classHasMethod(cl, selector))) {
                if (col != null) col.add(k);
                return true;
            }
        } catch (IndexOutOfBoundsException ex) {
            prev();
        }
        return false;
    }

    public boolean testEquality(final Object obj, final Collection<Object> col) {
        try {
            final Object k = data[next()];
            if (processNegate(Objects.equals(obj, k))) {
                if (col != null) col.add(k);
                return true;
            }
        } catch (IndexOutOfBoundsException ex) {
            prev();
        }
        return false;
    }

    public boolean testRange(final Comparable a, final Comparable b, final Collection<Object> col) {
        Object k = Epsilon.INSTANCE;
        try {
            // To be in range, either:
            //    a <= k and b >= k
            // or b <= k and a >= k
            // Which becomes
            // let u = a - k, v = b - k
            //    u <= 0 and v >= 0
            // or v <= 0 and u >= 0

            k = data[next()];
            final Comparable<?> ck = (Comparable<?>) k;
            final int u = compare(a, ck);
            final int v = compare(b, ck);
            if (processNegate(u <= 0 && v >= 0 || v <= 0 && u >= 0)) {
                if (col != null) col.add(k);
                return true;
            }
        } catch (IndexOutOfBoundsException ex) {
            prev();
        } catch (ClassCastException | NullPointerException ex) {
            // current slot value does not belong in set,
            // which satisfies *not* being in range.
            // return true if negate flag is on
            if (negateFlag) {
                if (col != null) col.add(k);
                return true;
            }
        }
        return false;
    }

    public boolean testSlotOccupied(Collection<Object> col) {
        try {
            final Object k = data[next()];
            if (!negateFlag) {
                if (col != null) col.add(k);
                return true;
            }
        } catch (IndexOutOfBoundsException ex) {
            prev();
        }
        return false;
    }

    public boolean testEnd(Collection<Object> col) {
        try {
            final Object k = data[next()];
            // out of bounds would have happened here if it had to happen
            // if it reaches beyond this point, that means, it is not the
            // end. Only happens when negate flag is on
            if (negateFlag) {
                if (col != null) col.add(k);
                return true;
            }
        } catch (IndexOutOfBoundsException ex) {
            prev();
            if (!negateFlag) {
                if (col != null) col.add(Epsilon.INSTANCE);
                return true;
            }
        }
        return false;
    }
}