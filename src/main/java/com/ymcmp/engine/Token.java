package com.ymcmp.engine;

import java.io.Serializable;

public final class Token<T extends Enum<T>> implements Serializable {

    private static final long serialVersionUID = 98266182463002L;

    public final T type;
    public final String text;

    public Token(T type, String text) {
        this.type = type;
        this.text = text == null ? "" : text;
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 13 + text.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj.getClass() == this.getClass()) {
            final Token t = (Token) obj;
            return type == t.type && text.equals(t.text);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Token[" + type + "]=" + text;
    }
}