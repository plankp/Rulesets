/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.lib;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;

public final class Reflectlib {

    private Reflectlib() {
        //
    }

    /**
     * if entry is list-like, we look for a method.
     * otherwise convert to string, look for field.
     *
     * This method of field *must* have public visibility
     *
     * @return
     *   - Null if error occurred (including entry does not exist),
     *   - Value if field named entry was found,
     *   - Function is method named entry was found,
     */
    public static Object access(Object base, Object entry) {
        if (base == null || entry == null) return null;
        final List<Object> list = convertToArray(entry);
        return list == null
                ? accessField(base, entry.toString())
                : accessMethod(base, list);
    }

    private static List<Object> convertToArray(Object obj) {
        final List<Object> list = Arraylib.polyArrayToList(obj);
        if (list != null) return list;
        if (obj instanceof Collection) return new ArrayList<>((Collection<?>) obj);
        return null;
    }

    private static Object accessField(Object obj, String name) {
        if (obj.getClass().isArray()) {
            // Arrays (at least in Java) only have length as a property
            // This property, however, cannot be found using getField
            return "length".equals(name) ? Arraylib.polyArraylength(obj) : null;
        }

        try {
            final Field field = obj.getClass().getField(name);
            return field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            return null;
        }
    }

    private static Object accessMethod(Object obj, List<Object> sel) {
        if (sel.isEmpty()) return null;
        final String name = sel.get(0).toString();
        final List<Object> params = sel.subList(1, sel.size());

        try {
            final Method method = obj.getClass().getMethod(name, params.stream().map(Object::getClass).toArray(Class[]::new));
            return method.invoke(obj, params.toArray());
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            return null;
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }
}