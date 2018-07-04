/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.IOException;
import java.io.StringReader;

import com.ymcmp.lexparse.IllegalParseException;

import org.junit.Test;
import static org.junit.Assert.fail;

public class IllegalParseTest {

    @Test(expected = IllegalParseException.class)
    public void parseBadRuleType() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("hello"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseRuleWithoutName() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseRuleWithoutClause() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = "))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseUnclosedAction() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = a { "))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseBadBinaryOperator() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = a { 10 + 3 - }"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseBadUnaryMinusValue() {
        // Fails because Abc is not numeric
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = -'\\\"Abc\\\'"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseEmptyValueAfterCharsOperator() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = %"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseBadValueAfterCharsOperator() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = %[]"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseSwitchRuleMissingRhs() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = a | b |"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseSubscriptMissingRhs() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = a { (1, 2, 3): }"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseAndMissingRhs() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = a { 1 & }"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseOrMissingRhs() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = a { 1 | }"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseAssignMissingRhs() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = a { b = }"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }

    @Test(expected = IllegalParseException.class)
    public void parseIgnoreMissingRhs() {
        try (final RsetLexer lexer = new RsetLexer(new StringReader("rule a = a { 10 ! }"))) {
            new RsetParser(lexer).parse();
        } catch (IOException ex) {
            fail("No manipulating IO?");
        }
    }
}