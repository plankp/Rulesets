/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

public class ByteClassLoader extends ClassLoader {

    public Class<?> loadFromBytes(String name, byte[] b) {
        return defineClass(name, b, 0, b.length);
    }
}