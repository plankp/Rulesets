/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

import java.util.Map;
import java.util.List;
import java.util.Stack;

import java.util.stream.Stream;
import java.util.stream.Collectors;

import java.util.function.Consumer;

import org.objectweb.asm.Type;
import org.objectweb.asm.Label;
import org.objectweb.asm.Handle;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import com.ymcmp.rset.ASMUtils;
import com.ymcmp.rset.Scope.VarType;

import com.ymcmp.rset.visitor.BytecodeRuleVisitor;
import com.ymcmp.rset.visitor.BytecodeActionVisitor;

import com.ymcmp.lexparse.tree.ParseTree;

import static org.objectweb.asm.Opcodes.*;

public final class RulesetGroup extends ParseTree {

    public List<RulesetNode> rsets;

    public RulesetGroup(List<RulesetNode> rsets) {
        this.rsets = rsets;
    }

    @Override
    public ParseTree getChild(int node) {
        return rsets.get(node);
    }

    @Override
    public int getChildCount() {
        return rsets.size();
    }

    @Override
    public String getText() {
        return '(' + "begin " + rsets.stream().map(ParseTree::getText)
                .collect(Collectors.joining(" ")) + ')';
    }

    public byte[] toBytecode(final String className) {
        return toBytecode(className, null, false);
    }

    public byte[] toBytecode(final String className, final String sourceFile, final boolean genDebugInfo) {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final Stack<String> fragmentStack = new Stack<>();

        final BytecodeActionVisitor aw = new BytecodeActionVisitor(cw, className);
        final BytecodeRuleVisitor rw = new BytecodeRuleVisitor(cw, className, genDebugInfo, rsets.stream()
                .collect(Collectors.toMap(e -> e.name.getText(), e -> {
                    switch (e.type) {
                        case RULE:
                        case SUBRULE:
                            return vis -> {
                                final String name = e.name.getText();
                                final int lst = vis.scope.findNearestLocal(VarType.LIST);
                                final int localEnv = vis.scope.pushNewLocal(VarType.MAP);
                                final int parseLst = vis.scope.pushNewLocal(VarType.LIST);
                                vis.mv.visitVarInsn(ALOAD, 0);
                                vis.mv.visitFieldInsn(GETFIELD, className, "ext", "Lcom/ymcmp/rset/lib/Extensions;");
                                vis.mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/lib/Extensions", "export", "()Ljava/util/Map;", false);
                                vis.mv.visitVarInsn(ASTORE, localEnv);
                                vis.newObjectNoArgs(parseLst, "java/util/ArrayList");
                                vis.mv.visitVarInsn(ALOAD, 0);
                                vis.mv.visitVarInsn(ALOAD, localEnv);
                                vis.mv.visitVarInsn(ALOAD, parseLst);
                                vis.mv.visitMethodInsn(INVOKEVIRTUAL, className, "test" + name, "(Ljava/util/Map;Ljava/util/List;)Z", false);
                                vis.mv.visitInsn(DUP);
                                vis.testIf(IFEQ, () -> {
                                    vis.mv.visitVarInsn(ALOAD, lst);
                                    vis.mv.visitVarInsn(ALOAD, 0);
                                    vis.mv.visitVarInsn(ALOAD, localEnv);

                                    vis.logMessage("FINER", "Executing action of " + name);

                                    vis.mv.visitMethodInsn(INVOKEVIRTUAL, className, "act" + name, "(Ljava/util/Map;)Ljava/lang/Object;", false);
                                    vis.mv.visitInsn(DUP);
                                    vis.testIf(IFNONNULL, () -> {
                                        vis.logMessage("FINER", "Using parse stack as result of action");
                                        vis.mv.visitInsn(POP);
                                        vis.mv.visitVarInsn(ALOAD, parseLst);
                                    });
                                    vis.mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                                    vis.mv.visitInsn(POP);
                                });
                                vis.mv.visitVarInsn(ISTORE, vis.RESULT);
                                vis.scope.popLocal();
                                vis.scope.popLocal();
                            };
                        case FRAGMENT:
                            return vis -> {
                                final String name = e.name.getText();
                                if (fragmentStack.contains(name)) {
                                    throw new RuntimeException("Recursive fragment definition via " + fragmentStack + " -> " + name);
                                }
                                fragmentStack.push(name);

                                vis.logMessage("FINE", "Entering rule fragment " + name);
                                vis.visit(e.rule);
                                vis.logMessage("FINE", "Exiting rule fragment " + name);

                                fragmentStack.pop();
                            };
                        default:
                            throw new RuntimeException("Unhandled ruleset type " + e.type);
                    }
                })));

        // Generating code for Java 8
        cw.visit(V1_8, ACC_PUBLIC | ACC_SUPER, className, null, "java/lang/Object", new String[]{
            "com/ymcmp/rset/rt/Rulesets"
        });

        if (sourceFile != null) cw.visitSource(sourceFile, null);

        {
            FieldVisitor fv;
            fv = cw.visitField(ACC_PRIVATE | ACC_FINAL, "rules", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Lcom/ymcmp/rset/rt/Rule;>;", null);
            fv.visitEnd();

            fv = cw.visitField(ACC_PUBLIC, "state", "Lcom/ymcmp/rset/rt/EvalState;", null, null);
            fv.visitEnd();

            fv = cw.visitField(ACC_PUBLIC, "ext", "Lcom/ymcmp/rset/lib/Extensions;", null, null);
            fv.visitEnd();
        }

        if (genDebugInfo) {
            // Constructor logger object
            final FieldVisitor fv = cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "LOGGER", "Ljava/util/logging/Logger;", null, null);
            fv.visitEnd();

            final MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitLdcInsn(Type.getType("L" + className + ";"));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/logging/Logger", "getLogger", "(Ljava/lang/String;)Ljava/util/logging/Logger;", false);
            mv.visitFieldInsn(PUTSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            // Dependency generating constructor, (when user does not have custom Extension to bundle)
            final MethodVisitor ctor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            final ASMUtils ctora = ASMUtils.wrapperFor(ctor);
            ctor.visitCode();
            ctor.visitVarInsn(ALOAD, 0);
            ctora.newObjectNoArgs(-1, "com/ymcmp/rset/lib/Extensions");
            ctor.visitMethodInsn(INVOKESPECIAL, className, "<init>", "(Lcom/ymcmp/rset/lib/Extensions;)V", false);
            ctor.visitInsn(RETURN);
            ctor.visitMaxs(0, 0);
            ctor.visitEnd();
        }

        final MethodVisitor ctor = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lcom/ymcmp/rset/lib/Extensions;)V", null, null);
        final ASMUtils ctora = ASMUtils.wrapperFor(ctor);
        ctor.visitCode();
        // super();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        // state = new EvalState();
        ctor.visitVarInsn(ALOAD, 0);
        ctora.newObjectNoArgs(-1, "com/ymcmp/rset/rt/EvalState");
        ctor.visitFieldInsn(PUTFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
        // ext = %injected through constructor;
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ALOAD, 1);
        ctor.visitFieldInsn(PUTFIELD, className, "ext", "Lcom/ymcmp/rset/lib/Extensions;");
        // rules = new HashMap<>();
        ctor.visitVarInsn(ALOAD, 0);
        ctora.newObjectNoArgs(-1, "java/util/HashMap");
        ctor.visitFieldInsn(PUTFIELD, className, "rules", "Ljava/util/Map;");

        for (final RulesetNode r : rsets) {
            final String ruleName;
            final String testName;
            final String actnName;

            final String name = r.name.getText();
            switch (r.type) {
                case RULE:
                    ruleName = "rule" + name;
                    testName = "test" + name;
                    actnName = "act" + name;
                    break;
                case SUBRULE:
                    ruleName = null;
                    testName = "test" + name;
                    actnName = "act" + name;
                    break;
                case FRAGMENT:
                default:
                    ruleName = null;
                    testName = null;
                    actnName = null;
                    break;
            }

            if (testName != null) rw.visit(r);
            if (actnName != null) aw.visit(r);

            if (ruleName == null) continue;

            final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, ruleName, "([Ljava/lang/Object;)Ljava/lang/Object;", null, null);
            final ASMUtils mva = ASMUtils.wrapperFor(mv);
            mv.visitCode();
            // state.reset(); state.setData(data@1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "reset", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "setData", "([Ljava/lang/Object;)V", false);
            // env@2 = ext.export();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "ext", "Lcom/ymcmp/rset/lib/Extensions;");
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

            // Map method to rule table
            ctor.visitVarInsn(ALOAD, 0);
            ctor.visitFieldInsn(GETFIELD, className, "rules", "Ljava/util/Map;");
            ctor.visitLdcInsn(name);
            ctor.visitVarInsn(ALOAD, 0);
            ctor.visitInvokeDynamicInsn("apply", "(L" + className + ";)Lcom/ymcmp/rset/rt/Rule;",
                    new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false),
                    new Object[]{Type.getType("(Ljava/lang/Object;)Ljava/lang/Object;"),
                            new Handle(H_INVOKEVIRTUAL, className, ruleName, "([Ljava/lang/Object;)Ljava/lang/Object;", false),
                            Type.getType("([Ljava/lang/Object;)Ljava/lang/Object;")
                    });
            ctor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            ctor.visitInsn(POP);
        }

        // Epilogue for constructor
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();

        // Implement the Rulesets interface
        {
            MethodVisitor mv;
            mv = cw.visitMethod(ACC_PUBLIC, "getRuleNames", "()Ljava/util/Set;", "()Ljava/util/Set<Ljava/lang/String;>;", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "rules", "Ljava/util/Map;");
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "keySet", "()Ljava/util/Set;", true);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = cw.visitMethod(ACC_PUBLIC, "getRule", "(Ljava/lang/String;)Lcom/ymcmp/rset/rt/Rule;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "rules", "Ljava/util/Map;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = cw.visitMethod(ACC_PUBLIC, "forEachRule", "(Ljava/util/function/BiConsumer;)V", "(Ljava/util/function/BiConsumer<-Ljava/lang/String;+Lcom/ymcmp/rset/rt/Rule;>;)V", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "rules", "Ljava/util/Map;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "forEach", "(Ljava/util/function/BiConsumer;)V", true);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();
        return cw.toByteArray();
    }
}