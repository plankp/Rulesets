/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.rt;

import java.util.Stack;
import java.util.Arrays;
import java.util.Objects;
import java.util.Collection;
import java.util.EmptyStackException;

public class EvalState {

    protected final Stack<Integer> indexes = new Stack<>();

    protected Object[] data;

    public EvalState(Object... data) {
        this.data = data;
    }

    public void setData(Object... data) {
        this.data = data;
    }

    public void reset() {
        data = new Object[0];
        indexes.clear();
        indexes.push(0);
    }

    public int next() {
        int i = 0;
        try {
            i = indexes.pop();
        } catch (EmptyStackException ex) {
            //
        }
        indexes.push(i + 1);
        return i;
    }

    public int prev() {
        int i = 0;
        try {
            i = indexes.pop();
        } catch (EmptyStackException ex) {
            //
        }
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

    public Object[] copyFromIndex(int from) {
        return copyRange(from, getIndex());
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

    public boolean testEquality(final Object obj) {
        return testEquality(obj, null);
    }

    public boolean testEquality(final Object obj, final Collection<Object> col) {
        try {
            final Object k = data[next()];
            if (Objects.equals(obj, k)) {
                if (col != null) col.add(k);
                return true;
            }
        } catch (IndexOutOfBoundsException ex) {
            prev();
        } catch (NullPointerException ex) {
            // return false
        }
        return false;
    }

    public boolean testRange(final Comparable a, final Comparable b) {
        return testRange(a, b, null);
    }

    public boolean testRange(final Comparable a, final Comparable b, final Collection<Object> col) {
        try {
            // To be in range, either:
            //    a <= k and b >= k
            // or b <= k and a >= k
            // Which becomes
            // let u = a - k, v = b - k
            //    u <= 0 and v >= 0
            // or v <= 0 and u >= 0

            final Object ko = data[next()];
            final int u = a.compareTo(ko);
            final int v = b.compareTo(ko);
            if (u <= 0 && v >= 0 || v <= 0 && u >= 0) {
                if (col != null) col.add(ko);
                return true;
            }
        } catch (IndexOutOfBoundsException ex) {
            prev();
        } catch (NullPointerException | ClassCastException ex) {
            // Ignore, return false
        }
        return false;
    }

    public boolean testSlotOccupied() {
        return testSlotOccupied(null);
    }

    public boolean testSlotOccupied(Collection<Object> col) {
        try {
            final Object k = data[next()];
            if (col != null) col.add(k);
            return true;
        } catch (IndexOutOfBoundsException ex) {
            prev();
        }
        return false;
    }

    public boolean testEnd() {
        return testEnd(null);
    }

    public boolean testEnd(Collection<Object> col) {
        try {
            ignore(data[next()]);
        } catch (IndexOutOfBoundsException ex) {
            prev();
            if (col != null) col.add(Epsilon.INSTANCE);
            return true;
        }
        return false;
    }

    public static <T> void ignore(T e) {
        // Literally do nothing
    }
}