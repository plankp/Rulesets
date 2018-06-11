package com.ymcmp.engine;

public interface Lexer<T extends Enum<T>> {

    public Token<T> nextToken();

    public static boolean isDigit(final int digit) {
        return digit >= '0' && digit <= '9';
    }

    public static boolean isIdent(final int id) {
        return id >= 'a' && id <= 'z'
            || id >= 'A' && id <= 'Z'
            || id == '$' || id == '_'
            || isDigit(id);
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