package com.ymcmp.rset;

import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;

public final class EvalRule {

    public static interface EvalFunc {

        public boolean eval(final EvalRule self, Map<String, Ruleset> env, EvalState eval);
    }

    public final Map<String, Object> captures = new HashMap<>();
    public final EvalFunc clause;

    public EvalRule(EvalFunc f) {
        this.clause = f;
    }

    public void reset() {
        captures.clear();
    }

    public boolean eval(Map<String, Ruleset> env, EvalState eval) {
        reset();
        return clause.eval(this, env, eval);
    }

    public boolean eval(EvalRule self, Map<String, Ruleset> env, EvalState eval) {
        return clause.eval(self, env, eval);
    }
}