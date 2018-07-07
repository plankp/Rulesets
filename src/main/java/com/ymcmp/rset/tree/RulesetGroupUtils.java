/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

import java.util.Optional;

import com.ymcmp.rset.ASMUtils;

import org.objectweb.asm.Type;
import org.objectweb.asm.Handle;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

/* package */ class RulesetGroupUtils {

    private RulesetGroupUtils() {
        //
    }

    public static void implGetRuleNames(ClassWriter cw, String className) {
        final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getRuleNames", "()Ljava/util/Set;", "()Ljava/util/Set<Ljava/lang/String;>;", null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "rules", "Ljava/util/Map;");
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "keySet", "()Ljava/util/Set;", true);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public static void implGetRule(ClassWriter cw, String className) {
        final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getRule", "(Ljava/lang/String;)Lcom/ymcmp/rset/rt/Rule;", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "rules", "Ljava/util/Map;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public static void implForEachRule(ClassWriter cw, String className) {
        final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "forEachRule", "(Ljava/util/function/BiConsumer;)V", "(Ljava/util/function/BiConsumer<-Ljava/lang/String;-Lcom/ymcmp/rset/rt/Rule;>;)V", null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "rules", "Ljava/util/Map;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "forEach", "(Ljava/util/function/BiConsumer;)V", true);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public static void generateRuleMethod(ClassWriter cw, final String className, final String name, final RulesetNode.Type type) {
        final String ruleName = type.ruleName(name).get();
        final String testName = type.testName(name).get();
        final String actnName = type.actnName(name).get();

        final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, ruleName, "([Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        final ASMUtils mva = ASMUtils.wrapperFor(mv);
        // state.reset(); state.setData(data@1);
        mva.selfGetField(className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "reset", "()V", false);
        mva.selfGetField(className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "setData", "([Ljava/lang/Object;)V", false);
        // env@2 = ext.export();
        mva.selfGetField(className, "ext", "Lcom/ymcmp/rset/lib/Extensions;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/lib/Extensions", "export", "()Ljava/util/Map;", false);
        mv.visitVarInsn(ASTORE, 2);
        // return (%test(env@2, new ArrayList<>())) ? act(env@2) : null;
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        mva.newObjectNoArgs(-1, "java/util/ArrayList");
        mv.visitMethodInsn(INVOKEVIRTUAL, className, testName, "(Ljava/util/Map;Ljava/util/List;)Z", false);
        mva.testIfElse(IFEQ, () -> {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, actnName, "(Ljava/util/Map;)Ljava/lang/Object;", false);
        }, () -> mv.visitInsn(ACONST_NULL));
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void initializeFields(final MethodVisitor ctor, final String className) {
        final ASMUtils ctora = ASMUtils.wrapperFor(ctor);
        // super();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        // state = new EvalState();
        ctora.newObjectNoArgs(-1, "com/ymcmp/rset/rt/EvalState");
        ctora.selfPutField(className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
        // ext = %injected through constructor;
        ctor.visitVarInsn(ALOAD, 1);
        ctora.selfPutField(className, "ext", "Lcom/ymcmp/rset/lib/Extensions;");
        // rules = new HashMap<>();
        ctora.newObjectNoArgs(-1, "java/util/HashMap");
        ctora.selfPutField(className, "rules", "Ljava/util/Map;");
    }

    public static void implDelegatingCtor(final MethodVisitor ctor, final String className, Iterable<RulesetNode> rsets) {
        initializeFields(ctor, className);

        for (final RulesetNode r : rsets) {
            r.makeRuleName().ifPresent(ruleName -> {
                // Map method to rule table
                ctor.visitVarInsn(ALOAD, 0);
                ctor.visitFieldInsn(GETFIELD, className, "rules", "Ljava/util/Map;");
                ctor.visitLdcInsn(r.name.getText());
                ctor.visitVarInsn(ALOAD, 0);
                ctor.visitInvokeDynamicInsn("apply", "(L" + className + ";)Lcom/ymcmp/rset/rt/Rule;",
                        new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false),
                        new Object[]{Type.getType("(Ljava/lang/Object;)Ljava/lang/Object;"),
                                new Handle(H_INVOKEVIRTUAL, className, ruleName, "([Ljava/lang/Object;)Ljava/lang/Object;", false),
                                Type.getType("([Ljava/lang/Object;)Ljava/lang/Object;")
                        });
                ctor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                ctor.visitInsn(POP);
            });
        }
    }
}