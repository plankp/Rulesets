package com.ymcmp.rset.rt;

import java.util.Set;
import java.util.function.BiConsumer;

public interface Rulesets {

    public Set<String> getRuleNames();

    public Rule getRule(String name);

    public void forEachRule(BiConsumer<? super String, ? extends Rule> consumer);
}