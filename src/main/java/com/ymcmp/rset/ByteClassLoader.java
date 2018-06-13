package com.ymcmp.rset;

public class ByteClassLoader extends ClassLoader {

    public Class<?> loadFromBytes(String name, byte[] b) {
        return defineClass(name, b, 0, b.length);
    }
}