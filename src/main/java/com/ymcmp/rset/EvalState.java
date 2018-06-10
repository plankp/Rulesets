package com.ymcmp.rset;

import java.util.Stack;
import java.util.Arrays;
import java.util.Objects;
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

    public void pop() {
        indexes.pop();
    }

    public void push() {
        indexes.push(getIndex());
    }

    public boolean testEquality(final Object obj) {
        try {
            final Object k = data[next()];
            if (Objects.equals(obj, k)) return true;
            if (Objects.equals(obj.toString(), k.toString())) return true;
        } catch (IndexOutOfBoundsException ex) {
            prev();
        } catch (NullPointerException ex) {
            // return false
        }
        return false;
    }

    public boolean testRange(final Comparable a, final Comparable b) {
        try {
            // To be in range, either:
            //    a <= k and b >= k
            // or b <= k and a >= k
            // Which becomes
            // let u = a - k, v = b - k
            //    u <= 0 and v >= 0
            // or v <= 0 and u >= 0

            final Object ko = data[next()];
            try {
                final int u = a.compareTo(ko);
                final int v = b.compareTo(ko);
                if (u <= 0 && v >= 0 || v <= 0 && u >= 0) return true;
            } catch (ClassCastException ex) {
                //
            }

            final String ks = ko.toString();
            final int u = a.toString().compareTo(ks);
            final int v = b.toString().compareTo(ks);
            if (u <= 0 && v >= 0 || v <= 0 && u >= 0) return true;
        } catch (IndexOutOfBoundsException ex) {
            prev();
        } catch (NullPointerException | ClassCastException ex) {
            // Ignore, return false
        }
        return false;
    }

    public boolean testSlotOccupied() {
        try {
            ignore(data[next()]);
            return true;
        } catch (IndexOutOfBoundsException ex) {
            prev();
        }
        return false;
    }

    public boolean testEnd() {
        try {
            ignore(data[next()]);
        } catch (IndexOutOfBoundsException ex) {
            prev();
            return true;
        }
        return false;
    }

    public static <T> void ignore(T e) {
        // Literally do nothing
    }
}