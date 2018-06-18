package com.ymcmp.rset;

public enum Type {
    L_INT, L_REAL, L_IDENT,
    S_CM,
    S_MN, S_AD,
    S_DV, S_MD,
    S_QM, S_EQ, S_CO,
    S_OR, S_TD, S_ST,
    S_AM, S_EX,
    S_LP, S_RP,
    S_LB, S_RB;

    public boolean isNumeric() {
        switch (this) {
            case L_INT:
            case L_REAL:
                return true;
            default:
                return false;
        }
    }
}