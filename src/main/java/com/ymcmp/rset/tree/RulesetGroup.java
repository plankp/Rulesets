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
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import com.ymcmp.rset.ASMUtils;
import com.ymcmp.rset.ClassWriterUtils;

import com.ymcmp.rset.Scope.VarType;

import com.ymcmp.rset.visitor.BytecodeRuleVisitor;
import com.ymcmp.rset.visitor.BytecodeActionVisitor;

import com.ymcmp.lexparse.tree.ParseTree;

import static org.objectweb.asm.Opcodes.*;

import static com.ymcmp.rset.tree.RulesetGroupUtils.*;

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
        final ClassWriterUtils cwa = new ClassWriterUtils(cw);

        final BytecodeActionVisitor aw = new BytecodeActionVisitor(cw, className);
        final BytecodeRuleVisitor rw = new BytecodeRuleVisitor(cw, className, genDebugInfo, generateRefsMap(className));

        // Generating code for Java 8
        cw.visitSource(sourceFile, null);
        cw.visit(V1_8, ACC_PUBLIC | ACC_SUPER, className, null, "java/lang/Object", new String[]{
            "com/ymcmp/rset/rt/Rulesets"
        });

        cwa.defineField(ACC_PRIVATE | ACC_FINAL, "rules", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Lcom/ymcmp/rset/rt/Rule;>;");
        cwa.defineField(ACC_PUBLIC, "state", "Lcom/ymcmp/rset/rt/EvalState;", null);
        cwa.defineField(ACC_PUBLIC, "ext", "Lcom/ymcmp/rset/lib/Extensions;", null);

        if (genDebugInfo) {
            // Construct logger object
            cwa.defineField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "LOGGER", "Ljava/util/logging/Logger;", null);
            cwa.defineStaticCtor(mv -> {
                mv.visitLdcInsn(Type.getType("L" + className + ";"));
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
                mv.visitMethodInsn(INVOKESTATIC, "java/util/logging/Logger", "getLogger", "(Ljava/lang/String;)Ljava/util/logging/Logger;", false);
                mv.visitFieldInsn(PUTSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
            });
        }

        // Dependency generating constructor, (when user does not have custom Extension to bundle)
        cwa.defineCtor(ACC_PUBLIC, "()V", ctor -> {
            final ASMUtils ctora = ASMUtils.wrapperFor(ctor);
            ctor.visitVarInsn(ALOAD, 0);
            ctora.newObjectNoArgs(-1, "com/ymcmp/rset/lib/Extensions");
            ctor.visitMethodInsn(INVOKESPECIAL, className, "<init>", "(Lcom/ymcmp/rset/lib/Extensions;)V", false);
        });

        // The delegating constructor (the one doing all the work!)
        cwa.defineCtor(ACC_PUBLIC, "(Lcom/ymcmp/rset/lib/Extensions;)V", ctor -> implDelegatingCtor(ctor, className, rsets));

        for (final RulesetNode r : rsets) {
            r.makeTestName().ifPresent(k -> rw.visit(r));
            r.makeActnName().ifPresent(k -> aw.visit(r));
            r.makeRuleName().ifPresent(k -> generateRuleMethod(cw, className, r.name.getText(), r.type));
        }

        // Implement the Rulesets interface
        implGetRule(cw, className);
        implGetRuleNames(cw, className);
        implForEachRule(cw, className);

        cw.visitEnd();
        return cw.toByteArray();
    }

    private Map<String, Consumer<BytecodeRuleVisitor>> generateRefsMap(final String className) {
        final Stack<String> fragmentStack = new Stack<>();
        return rsets.stream().collect(Collectors.toMap(e -> e.name.getText(), e -> {
            switch (e.type) {
                case RULE:
                case SUBRULE:
                    return vis -> {
                        final String name = e.name.getText();
                        final int lst = vis.scope.findNearestLocal(VarType.LIST);
                        final int localEnv = vis.scope.pushNewLocal(VarType.MAP);
                        final int parseLst = vis.scope.pushNewLocal(VarType.LIST);
                        vis.selfGetField(className, "ext", "Lcom/ymcmp/rset/lib/Extensions;");
                        vis.mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/lib/Extensions", "export", "()Ljava/util/Map;", false);
                        vis.mv.visitVarInsn(ASTORE, localEnv);
                        vis.newObjectNoArgs(parseLst, "java/util/ArrayList");
                        vis.mv.visitVarInsn(ALOAD, 0);
                        vis.mv.visitVarInsn(ALOAD, localEnv);
                        vis.mv.visitVarInsn(ALOAD, parseLst);
                        vis.mv.visitMethodInsn(INVOKEVIRTUAL, className, e.makeTestName().get(), "(Ljava/util/Map;Ljava/util/List;)Z", false);
                        vis.mv.visitInsn(DUP);
                        vis.testIf(IFEQ, () -> {
                            vis.mv.visitVarInsn(ALOAD, lst);
                            vis.mv.visitVarInsn(ALOAD, 0);
                            vis.mv.visitVarInsn(ALOAD, localEnv);

                            vis.logMessage("FINER", "Executing action of " + name);

                            vis.mv.visitMethodInsn(INVOKEVIRTUAL, className, e.makeActnName().get(), "(Ljava/util/Map;)Ljava/lang/Object;", false);
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
        }));
    }
}