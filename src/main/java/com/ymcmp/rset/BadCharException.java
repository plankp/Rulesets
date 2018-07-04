/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

public class BadCharException extends RuntimeException {

    public BadCharException(char c) {
        super("Unknown char " + c);
    }
}