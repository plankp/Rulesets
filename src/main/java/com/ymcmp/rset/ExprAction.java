package com.ymcmp.rset;

import java.util.Map;

public interface ExprAction {

    public Object apply(Map<String, Object> env);
}