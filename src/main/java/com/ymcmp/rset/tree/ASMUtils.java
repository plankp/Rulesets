package com.ymcmp.rset.tree;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

final class ASMUtils {

    private ASMUtils() {
        //
    }

    public static void newObjectNoArgs(final MethodVisitor mv, final String className, int slot) {
        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
        if (slot > 0) mv.visitVarInsn(ASTORE, slot);
    }

    public static void testIfElse(final MethodVisitor mv, final int jumpInsn, final Runnable ifTrue, final Runnable ifFalse) {
        final Label br0 = new Label();
        final Label br1 = new Label();
        mv.visitJumpInsn(jumpInsn, br0);
        ifTrue.run();
        mv.visitJumpInsn(GOTO, br1);
        mv.visitLabel(br0);
        ifFalse.run();
        mv.visitLabel(br1);
    }

    public static void testIf(final MethodVisitor mv, final int jumpInsn, final Runnable ifTrue) {
        final Label br0 = new Label();
        mv.visitJumpInsn(jumpInsn, br0);
        ifTrue.run();
        mv.visitLabel(br0);
    }
}