/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.util.Stack;

public class Scope {

    public enum VarType {
        HIDDEN, MAP, LIST, NUM, BOOL, EVAL_STATE;
    }

    private final Stack<VarType> locals = new Stack<>();

    public int pushNewLocal(VarType t) {
        locals.push(t);
        return locals.size() - 1;
    }

    public void popLocal() {
        locals.pop();
    }

    public int findNearestLocal(VarType t) {
        return locals.lastIndexOf(t);
    }
}