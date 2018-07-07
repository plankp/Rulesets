/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.rt;

import java.util.Set;
import java.util.function.BiConsumer;

public interface Rulesets {

    public Set<String> getRuleNames();

    public Rule getRule(String name);

    public void forEachRule(BiConsumer<? super String, ? super Rule> consumer);
}