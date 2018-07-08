/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.lib;

import java.util.List;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArraylibTest {

    private Object[] toArray(Object... arr) {
        return arr;
    }

    @Test
    public void reverseDoesNotPerformInPlace() {
        assertNull(Arraylib.reverse(null));

        final Object[] array = toArray(1, 2, 3);
        assertArrayEquals(toArray(3, 2, 1), (Object[]) Arraylib.reverse(array));
        assertArrayEquals(toArray(1, 2, 3), array);

        final List<Object> list = Arrays.asList(1, 2, 3);
        assertEquals(Arrays.asList(3, 2, 1), Arraylib.reverse(list));
        assertEquals(Arrays.asList(1, 2, 3), list);

        // Test it anyway even though strings are immutable in java
        final String string = "hello";
        assertEquals("olleh", Arraylib.reverse(string));
        assertEquals("hello", string);
    }

    @Test
    public void sortDoesNotPerformInPlace() {
        assertNull(Arraylib.sort(null));

        final Object[] array = toArray(2, 1, 3);
        assertArrayEquals(toArray(1, 2, 3), (Object[]) Arraylib.sort(array));
        assertArrayEquals(toArray(2, 1, 3), array);

        final List<Object> list = Arrays.asList(2, 1, 3);
        assertEquals(Arrays.asList(1, 2, 3), Arraylib.sort(list));
        assertEquals(Arrays.asList(2, 1, 3), list);

        // Test it anyway even though strings are immutable in java
        final String string = "hello";
        assertEquals("ehllo", Arraylib.sort(string));
        assertEquals("hello", string);
    }

    @Test
    public void iotaGenerates1ToInclusiveN() {
        assertArrayEquals(new int[]{1, 2, 3}, Arraylib.iota(3));
    }

    @Test
    public void lengthWorksOnAllArrays() {
        assertEquals(3, Arraylib.length(new byte[]{1, 2, 3}));
        assertEquals(3, Arraylib.length(new short[]{1, 2, 3}));
        assertEquals(3, Arraylib.length(new char[]{'a', 'b', 'c'}));
        assertEquals(3, Arraylib.length(new int[]{1, 2, 3}));
        assertEquals(3, Arraylib.length(new float[]{1, 2, 3}));
        assertEquals(3, Arraylib.length(new long[]{1, 2, 3}));
        assertEquals(3, Arraylib.length(new double[]{1, 2, 3}));
        assertEquals(3, Arraylib.length(new Object[]{null, null, null}));
    }

    @Test
    public void subscriptWorksOnAllArrays() {
        assertEquals((byte) 3, Arraylib.subscript(new byte[]{1, 2, 3}, 2));
        assertEquals((short) 3, Arraylib.subscript(new short[]{1, 2, 3}, 2));
        assertEquals('b', Arraylib.subscript(new char[]{'a', 'b', 'c'}, 1));
        assertEquals((int) 3, Arraylib.subscript(new int[]{1, 2, 3}, 2));
        assertEquals((float) 3, Arraylib.subscript(new float[]{1, 2, 3}, 2));
        assertEquals((long) 2, Arraylib.subscript(new long[]{1, 2, 3}, 1));
        assertEquals((double) 1, Arraylib.subscript(new double[]{1, 2, 3}, 0));
        assertEquals("Hi", Arraylib.subscript(new Object[]{null, "Hi", null}, 1));
    }

    @Test
    public void toIterableReturnsNullIfInputIsNull() {
        assertNull(Arraylib.toIterable(null));
    }

    @Test
    public void testToIterable() {
        int counter = 0;
        for (final Object el : Arraylib.toIterable(toArray(1, 2, 3))) {
            ++counter;
        }
        assertEquals(3, counter);

        counter = 0;
        for (final Object el : Arraylib.toIterable(Arrays.asList(1, 2, 3))) {
            ++counter;
        }
        assertEquals(3, counter);

        counter = 0;
        for (final Object el : Arraylib.toIterable("Abc")) {
            ++counter;
        }
        assertEquals(3, counter);

        counter = 0;
        for (final Object el : Arraylib.toIterable(new int[] {2, 7, 4})) {
            ++counter;
        }
        assertEquals(3, counter);
    }
}