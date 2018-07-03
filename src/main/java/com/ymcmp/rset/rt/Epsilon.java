/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.rt;

public final class Epsilon {

    private static final long serialVersionUID = 198375993712L;

    public static final Epsilon INSTANCE = new Epsilon();

    private Epsilon() {
        //
    }

    @Override
    public boolean equals(final Object k) {
        // Singleton, only possibility for equality
        return k == this;
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