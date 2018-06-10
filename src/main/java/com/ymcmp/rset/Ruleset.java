package com.ymcmp.rset;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ymcmp.rset.lib.Extensions;

public class Ruleset {

    private final String name;
    private final EvalRule rule;

    private ExprAction action;

    public Ruleset(String name, EvalRule rule) {
        this.name = name;
        this.rule = rule;
    }

    public String getName() {
        return name;
    }

    public EvalRule getRule() {
        return rule;
    }

    public ExprAction getAction() {
        return action;
    }

    public void setAction(final ExprAction act) {
        this.action = act;
    }

    public static Map<String, Ruleset> toEvalMap(List<Ruleset> rsets) {
        return rsets.stream()
                .filter(e -> e != null)
                .collect(Collectors.toMap(Ruleset::getName, e -> e));
    }

    public static Map<String, Optional<Object>> evalute(final Map<String, Ruleset> rules, final Extensions ext, final Object... data) {
        return rules.entrySet().stream()
                .filter(e -> e.getValue().getRule().eval(rules, new EvalState(data)))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    final Ruleset rset = e.getValue();
                    if (rset.getAction() == null) return Optional.empty();

                    final Map<String, Object> env = ext.export();
                    env.putAll(rset.getRule().captures);
                    return Optional.ofNullable(rset.getAction().apply(env));
                }));
    }
}
