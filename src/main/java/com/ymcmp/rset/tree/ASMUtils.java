/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public interface ASMUtils {

    public MethodVisitor getMethodVisitor();

    public default void newObjectNoArgs(int slot, final String className) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
        if (slot > 0) mv.visitVarInsn(ASTORE, slot);
    }

    public default void testIfElse(final int jumpInsn, final Runnable ifTrue, final Runnable ifFalse) {
        final MethodVisitor mv = getMethodVisitor();
        final Label br0 = new Label();
        final Label br1 = new Label();
        mv.visitJumpInsn(jumpInsn, br0);
        ifTrue.run();
        mv.visitJumpInsn(GOTO, br1);
        mv.visitLabel(br0);
        ifFalse.run();
        mv.visitLabel(br1);
    }

    public default void testIf(final int jumpInsn, final Runnable ifTrue) {
        final MethodVisitor mv = getMethodVisitor();
        final Label br0 = new Label();
        mv.visitJumpInsn(jumpInsn, br0);
        ifTrue.run();
        mv.visitLabel(br0);
    }

    public static void newObjectNoArgs(final MethodVisitor vis, int slot, final String className) {
        new ASMUtils() {
            @Override
            public MethodVisitor getMethodVisitor() {
                return vis;
            }
        }.newObjectNoArgs(slot, className);
    }

    public static void testIfElse(final MethodVisitor vis, final int jumpInsn, final Runnable ifTrue, final Runnable ifFalse) {
        new ASMUtils() {
            @Override
            public MethodVisitor getMethodVisitor() {
                return vis;
            }
        }.testIfElse(jumpInsn, ifTrue, ifFalse);
    }

    public static void testIf(final MethodVisitor vis, final int jumpInsn, final Runnable ifTrue) {
        new ASMUtils() {
            @Override
            public MethodVisitor getMethodVisitor() {
                return vis;
            }
        }.testIf(jumpInsn, ifTrue);
    }
}