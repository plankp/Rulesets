/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.IOException;
import java.io.StringReader;

import com.ymcmp.rset.BadCharException;

import org.junit.Test;
import static org.junit.Assert.fail;

public class IllegalLexTest {

    @Test(expected = BadCharException.class)
    public void lexIllegalCharacter() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("\0"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }
}