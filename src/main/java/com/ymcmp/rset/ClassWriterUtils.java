/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.util.function.Consumer;

import org.objectweb.asm.Type;
import org.objectweb.asm.Label;
import org.objectweb.asm.Handle;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class ClassWriterUtils {

    private final ClassWriter cw;

    public ClassWriterUtils(ClassWriter cw) {
        this.cw = cw;
    }

    public void defineField(int flags, String name, String type, String optDesc) {
        final FieldVisitor fv = cw.visitField(flags, name, type, optDesc, null);
        fv.visitEnd();
    }

    public void defineStaticCtor(Consumer<? super MethodVisitor> body) {
        final MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        body.accept(mv);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public void defineCtor(int flags, String params, Consumer<? super MethodVisitor> body) {
        final MethodVisitor mv = cw.visitMethod(flags, "<init>", params, null, null);
        body.accept(mv);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}