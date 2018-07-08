/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.lib;

import java.util.HashMap;
import java.util.ArrayList;

import org.junit.Test;

import static org.junit.Assert.*;

public class StdlibTest {

    private Object[] toArray(Object... arr) {
        return arr;
    }

    @Test
    public void testToOrdinal() {
        final Object[] ret = Stdlib.toOrdinal(null, 'a', "A", "", 10, new Object[0]);
        assertArrayEquals(toArray(null, (int) 'a', (int) 'A', 0, 10, null), ret);
    }

    @Test
    public void testToChar() {
        final Object[] ret = Stdlib.toChar(null, 'a', 65, new Object[0]);
        assertArrayEquals(toArray(null, 'a', (char) 65, null), ret);
    }

    @Test
    public void testToInt() {
        final Object[] ret = Stdlib.toInt(null, 'a', 6.5, "10", "a", new Object[0]);
        assertArrayEquals(toArray(null, (int) 'a', (int) 6.5, 10, null, null), ret);
    }

    @Test
    public void testToFloat() {
        final Object[] ret = Stdlib.toFloat(null, 'a', 6.5, "10", "a", new Object[0]);
        assertArrayEquals(toArray(null, null, 6.5, 10.0, null, null), ret);
    }

    @Test
    public void testJoin() {
        final Object[] data = toArray(null, toArray(), 'a', new HashMap(), 1, new ArrayList(), 5, toArray(1), "Hello");
        assertEquals("null a 1 5 1 Hello", Stdlib.join(data));
    }

    @Test
    public void joinIsSpecialCaseOfJoinWithDelim() {
        final Object[] data = toArray(null, toArray(), "Hello");
        assertEquals(Stdlib.join(data), Stdlib.joinWithDelim(" ", data));
    }

    @Test
    public void concatIsSpecialCaseOfJoinWithDelim() {
        final Object[] data = toArray(null, toArray(), "Hello");
        assertEquals(Stdlib.concat(data), Stdlib.joinWithDelim("", data));
    }

    @Test
    public void linesIsSpecialCaseOfJoinWithDelim() {
        final Object[] data = toArray(null, toArray(), "Hello");
        assertEquals(Stdlib.lines(data), Stdlib.joinWithDelim("\n", data));
    }

    @Test
    public void ignoreReturnsLastValueWhenPossible() {
        assertNull(Stdlib.ignore());
        assertEquals("c", Stdlib.ignore("a", "b", "c"));
    }

    @Test
    public void equalsReturnsTrueOnlyIfAllInputsAreEqual() {
        assertTrue(Stdlib.equals("a", "a", "a"));
        assertFalse(Stdlib.equals("a", "b", "a"));
        assertFalse(Stdlib.equals("a", "a", "b"));
    }

    @Test
    public void testPredefinedTrueObjects() {
        assertFalse(Stdlib.isTruthy(null));
        assertTrue(Stdlib.isTruthy(true));
        assertFalse(Stdlib.isTruthy(false));
        assertTrue(Stdlib.isTruthy(1));
        assertFalse(Stdlib.isTruthy(0));
        assertTrue(Stdlib.isTruthy((char) 1));
        assertFalse(Stdlib.isTruthy((char) 0));
        assertTrue(Stdlib.isTruthy(toArray(1)));
        assertFalse(Stdlib.isTruthy(toArray()));
        assertTrue(Stdlib.isTruthy("Abc"));
        assertFalse(Stdlib.isTruthy(""));
        assertTrue(Stdlib.isTruthy(new int[4]));
    }
}