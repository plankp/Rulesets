/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.IOException;
import java.io.StringReader;

import com.ymcmp.lexparse.IllegalParseException;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.fail;

public class IllegalContextTest {

    private Main.Options opt;

    @Before
    public void initOptions() {
        opt = new Main.Options();
        opt.className = "Name";
        opt.pathName = ".";
        opt.inputName = "memory";
        opt.outputName = "Name.class";
    }

    @Test(expected = RuntimeException.class)
    public void visitUndefinedRule() {
        try {
            Main.compile(new StringReader("rule a = &b"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitNullClassName() {
        try {
            Main.compile(new StringReader("rule a = <()"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitNullSelectorName() {
        try {
            Main.compile(new StringReader("rule a = !()"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitIllegalChar1Range() {
        try {
            // Char range only works on singular characters
            Main.compile(new StringReader("rule a = %abc-%d"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitIllegalChar2Range() {
        try {
            // Char range only works on singular characters
            Main.compile(new StringReader("rule a = %''-%d"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitIllegalNullRangeName() {
        try {
            Main.compile(new StringReader("rule a = ()-a"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitDirectRecursiveFragment() {
        try {
            Main.compile(new StringReader("rule a = &b, fragment b = &b"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitMutualRecursiveFragment() {
        try {
            Main.compile(new StringReader("rule a = &b, fragment b = &c, fragment c = &b"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitFragmentWithNonEmptyActionBody() {
        try {
            Main.compile(new StringReader("fragment a = a { 'Hi!' }"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitNullSubstitution() {
        try {
            Main.compile(new StringReader("fragment a = &0, rule b = &a"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitOutOfBoundsSubstitution() {
        try {
            Main.compile(new StringReader("fragment a = &3, rule b = &a/1/2"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitSubstitutionOnRule() {
        try {
            Main.compile(new StringReader("rule a = &0"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }

    @Test(expected = RuntimeException.class)
    public void visitSubstitutionOnSubrule() {
        try {
            Main.compile(new StringReader("subrule a = &0"), opt);
        } catch (IOException ex) {
            fail("No IO operation!");
        }
    }
}