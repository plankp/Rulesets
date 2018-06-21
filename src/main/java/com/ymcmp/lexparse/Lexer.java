/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.lexparse;

import java.util.function.IntPredicate;

public interface Lexer<T extends Enum<T>> {

    public Token<T> nextToken();

    public int read();

    public void unread(int k);

    public default String readWhile(IntPredicate pred) {
        final StringBuilder sb = new StringBuilder();
        int k;
        while (pred.test(k = read())) {
            sb.append((char) k);
        }
        unread(k);
        return sb.toString();
    }

    public static boolean isEOL(final int eol) {
        switch (eol) {
            case -1:
            case '\n':
            case '\r':
                return true;
            default:
                return false;
        }
    }
}