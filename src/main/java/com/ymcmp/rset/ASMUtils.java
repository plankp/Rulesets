/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

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
        testIf(jumpInsn, new Label(), ifTrue);
    }

    public default void testIf(final int jumpInsn, final Label exit, final Runnable ifTrue) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitJumpInsn(jumpInsn, exit);
        ifTrue.run();
        mv.visitLabel(exit);
    }

    public default void whileLoop(final Consumer<? super Label> test, final BiConsumer<? super Label, ? super Label> body) {
        final MethodVisitor mv = getMethodVisitor();
        final Label loop = new Label();
        final Label exit = new Label();
        mv.visitLabel(loop);
        test.accept(exit);
        if (body != null) body.accept(exit, loop);
        mv.visitJumpInsn(GOTO, loop);
        mv.visitLabel(exit);
    }

    public static ASMUtils wrapperFor(final MethodVisitor vis) {
        return new ASMUtils() {
            @Override
            public MethodVisitor getMethodVisitor() {
                return vis;
            }
        };
    }

    public default void ifBoolTrue(int boolSlot, Runnable body) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitVarInsn(ILOAD, boolSlot);
        testIf(IFEQ, body);
    }

    public default void ifBoolFalse(int boolSlot, Runnable body) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitVarInsn(ILOAD, boolSlot);
        testIf(IFNE, body);
    }

    public default void ifBoolFalse(int boolSlot, Label lbl, Runnable body) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitVarInsn(ILOAD, boolSlot);
        testIf(IFNE, lbl, body);
    }

    public default void jumpIfBoolFalse(int boolSlot, Label dest) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitVarInsn(ILOAD, boolSlot);
        mv.visitJumpInsn(IFEQ, dest);
    }

    public default void ifBoolElse(int boolSlot, Runnable ifTrue, Runnable ifFalse) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitVarInsn(ILOAD, boolSlot);
        testIfElse(IFEQ, ifTrue, ifFalse);
    }
}