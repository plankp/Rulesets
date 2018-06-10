package com.ymcmp.rset;

import java.io.Serializable;

public final class Token implements Serializable {

    private static final long serialVersionUID = 98266182463002L;

    public enum Type {
        L_NUMBER, L_IDENT,
        S_CM,
        S_MN, S_AD, S_QM,
        S_EQ, S_CO,
        S_OR, S_TD, S_ST,
        S_AM, S_EX,
        S_LP, S_RP,
        S_LB, S_RB;
    }

    public final Type type;
    public final String text;

    public Token(Type type, String text) {
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