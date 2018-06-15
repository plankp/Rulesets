package com.ymcmp.rset.rt;

public final class Epsilon {

    private static final long serialVersionUID = 198375993712L;

    public static final Epsilon INSTANCE = new Epsilon();

    private Epsilon() {
        //
    }

    @Override
    public boolean equals(final Object k) {
        return k != null && k == this;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(serialVersionUID);
    }

    @Override
    public String toString() {
        return "<epsilon>";
    }
}