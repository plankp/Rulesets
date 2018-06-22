/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.lib;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import java.util.function.Function;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

public final class Extensions {

    public static final int EXT_STDLIB = 1 << 0;
    public static final int EXT_MATH   = 1 << 1;
    public static final int EXT_ARRAY  = 1 << 2;

    public static final int ENABLE_ALL = EXT_STDLIB | EXT_MATH | EXT_ARRAY;

    private final int featureMask;
    private final Map<String, Object> imported;

    public Extensions() {
        this(ENABLE_ALL);
    }

    public Extensions(final int featureMask) {
        this.featureMask = featureMask;
        this.imported = new HashMap<>();
    }

    public int getEnabledFeatures() {
        return this.featureMask;
    }

    public Map<String, Object> export() {
        final Map<String, Object> map = new HashMap<>();
        exportTo(map);
        return map;
    }

    public void exportTo(Map<String, Object> module) {
        if ((featureMask & EXT_STDLIB) == EXT_STDLIB) {
            exportClassTo(Stdlib.class, module);
        }

        if ((featureMask & EXT_MATH) == EXT_MATH) {
            exportClassTo(Mathlib.class, module);
        }

        module.putAll(imported);
    }

    public void importClass(Class<?> cl) {
        exportClassTo(cl, imported);
    }

    public static void exportClassTo(Class<?> cl, Map<String, Object> module) {
        if (cl == null) return;

        // Make sure if it is static
        for (final Field field : cl.getFields()) {
            if (!field.isAnnotationPresent(Export.class)) continue;

            String name = field.getAnnotation(Export.class).value();
            if (name == null || name.isEmpty()) name = field.getName();

            try {
                if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
                    module.put(name, field.get(null));
                } else {
                    // need object reference, convert into function call: self -> self.field
                    module.put(name, new Function<Object[], Object>() {
                        @Override
                        public Object apply(final Object[] args) {
                            try {
                                return field.get(args[0]);
                            } catch (IllegalAccessException ex) {
                                throw new RuntimeException("Interface to " + cl.getSimpleName() + "#" + field.getName() + " failed", ex);
                            }
                        }
                    });
                }
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Interface to " + cl.getSimpleName() + "." + field.getName() + " failed", ex);
            }
        }

        for (final Method method : cl.getMethods()) {
            if (!method.isAnnotationPresent(Export.class)) continue;

            String name = method.getAnnotation(Export.class).value();
            if (name == null || name.isEmpty()) name = method.getName();

            module.put(name, new Function<Object[], Object>() {
                @Override
                public Object apply(final Object[] rargs) {
                    Object self = null;
                    try {
                        Object[] args;
                        if ((method.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
                            args = rargs;
                        } else {
                            // First parameter is treated as *self*
                            self = rargs[0];
                            args = Arrays.copyOfRange(rargs, 1, rargs.length);
                        }

                        if (method.isVarArgs()) {
                            final int spec = method.getParameterCount() - 1;
                            if (spec < 0) {
                                throw new RuntimeException("Wtf? Varargs method without parameter slot?");
                            }
                            if (spec == 0) {
                                return method.invoke(self, (Object) args);
                            } else {
                                final Object[] fixedArgs = new Object[spec + 1];
                                // Process non-varargs parameters
                                System.arraycopy(args, 0, fixedArgs, 0, spec);
                                // Process varargs parameters as Object[]
                                fixedArgs[spec] = Arrays.copyOfRange(args, spec, args.length);
                                return method.invoke(self, fixedArgs);
                            }
                        } else {
                            return method.invoke(self, args);
                        }
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        throw new RuntimeException("Interface to " + cl.getSimpleName() + (self == null ? "." : "#") + method.getName() + "(?) failed", ex);
                    }
                }
            });
        }
    }
}