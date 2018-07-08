/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.lib;

import java.util.List;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

public class MathlibTest {

    @Test
    public void testAdd() {
        assertEquals(15, Mathlib.add(1, 2, 3, 4, 5), 0.0000001);
    }

    @Test
    public void testSub() {
        assertEquals(-13, Mathlib.sub(1, 2, 3, 4, 5), 0.0000001);
    }

    @Test
    public void testMul() {
        assertEquals(120, Mathlib.mul(1, 2, 3, 4, 5), 0.0000001);
    }

    @Test
    public void testDiv() {
        assertEquals(1.0 / 2 / 3 / 4 / 5, Mathlib.div(1, 2, 3, 4, 5), 0.0000001);
    }

    @Test
    public void testMod() {
        assertEquals(1.0 % 2 % 3 % 4 % 5, Mathlib.mod(1, 2, 3, 4, 5), 0.0000001);
    }

    @Test
    public void testAnd() {
        assertEquals(1 & 2 & 3 & 4 & 5, Mathlib.and(1, 2, 3, 4, 5));
    }

    @Test
    public void testOr() {
        assertEquals(1 | 2 | 3 | 4 | 5, Mathlib.or(1, 2, 3, 4, 5));
    }

    @Test
    public void testXor() {
        assertEquals(1 ^ 2 ^ 3 ^ 4 ^ 5, Mathlib.xor(1, 2, 3, 4, 5));
    }

    @Test
    public void testNot() {
        assertTrue((Boolean) Mathlib.not(false));
        assertFalse((Boolean) Mathlib.not(true));
        assertEquals(~1, Mathlib.not(1));
        assertEquals(~1, Mathlib.not(1.0));
        assertNull(Mathlib.not(this));
    }
}