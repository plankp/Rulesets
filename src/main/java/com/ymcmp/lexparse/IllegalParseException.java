/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.lexparse;

public class IllegalParseException extends RuntimeException {

    public IllegalParseException(Token<?> t) {
        super("Unexpected " + t);
    }

    public IllegalParseException(String message) {
        super(message);
    }

    public IllegalParseException(String message, Token<?> t) {
        super(message + (t == null ? "" : " (Found " + t + ")"));
    }
}