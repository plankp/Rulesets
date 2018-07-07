/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import com.ymcmp.rset.tree.ValueNode;

import com.ymcmp.function.TriConsumer;

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

    public default void selfGetField(String className, String fieldName, String type) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, fieldName, type);
    }

    public default void selfPutField(String className, String fieldName, String type) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(SWAP);
        mv.visitFieldInsn(PUTFIELD, className, fieldName, type);
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
        body.accept(exit, loop);
        mv.visitJumpInsn(GOTO, loop);
        mv.visitLabel(exit);
    }

    public default void fixedIntCounter(int idx, int lower, int upper, TriConsumer<Integer, Label, Label> body) {
        /*
        for ($idx = $lower, $idx < $upper; ++$idx) {
            $body
        }
        
        $idx = $lower - 1
    loop:
        $idx++  ; put this here so GOTO loop behaves like continue in a for loop
        goto exit if $idx >= $upper
        $body.accept($idx, exit, loop)
        goto loop
    exit:
        */

        final MethodVisitor mv = getMethodVisitor();

        mv.visitLdcInsn(lower - 1);
        mv.visitVarInsn(ISTORE, idx);

        whileLoop(exit -> {
            mv.visitIincInsn(idx, 1);
            mv.visitVarInsn(ILOAD, idx);
            mv.visitLdcInsn(upper);
            mv.visitJumpInsn(IF_ICMPGE, exit);
        }, (exit, loop) -> body.accept(idx, exit, loop));
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

    public default void jumpIfBoolTrue(int boolSlot, Label dest) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitVarInsn(ILOAD, boolSlot);
        mv.visitJumpInsn(IFNE, dest);
    }

    public default void ifBoolElse(int boolSlot, Runnable ifTrue, Runnable ifFalse) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitVarInsn(ILOAD, boolSlot);
        testIfElse(IFEQ, ifTrue, ifFalse);
    }

    public default void pushAsObject(final ValueNode n) {
        final MethodVisitor mv = getMethodVisitor();

        if (n.token.type == Type.L_NULL) {
            mv.visitInsn(ACONST_NULL);
            return;
        }

        mv.visitLdcInsn(n.toObject());

        // Perform necessary boxing or conversions
        switch (n.token.type) {
            case L_INT:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case L_REAL:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            case L_CHARS:
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false);
                break;
        }
    }

    public default void storeBool(int slot, boolean value) {
        final MethodVisitor mv = getMethodVisitor();
        mv.visitInsn(value ? ICONST_1 : ICONST_0);
        mv.visitVarInsn(ISTORE, slot);
    }
}