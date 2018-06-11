package com.ymcmp.rset;

import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;

import com.ymcmp.function.TriPredicate;

public final class EvalRule {

    @FunctionalInterface
    public static interface EvalFunc extends TriPredicate<EvalRule, Map<String, Ruleset>, EvalState> {

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
        return clause.test(this, env, eval);
    }

    public boolean eval(EvalRule self, Map<String, Ruleset> env, EvalState eval) {
        return clause.test(self, env, eval);
    }
}