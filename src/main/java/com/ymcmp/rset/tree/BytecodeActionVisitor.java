/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import com.ymcmp.rset.Type;

import com.ymcmp.lexparse.tree.Visitor;
import com.ymcmp.lexparse.tree.ParseTree;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeActionVisitor extends Visitor<Void> {

    private ClassWriter cw;
    private String className;
    private MethodVisitor mv;
    private int locals;

    public BytecodeActionVisitor(ClassWriter cw, String className) {
        this.cw = cw;
        this.className = className;
    }

    public Void visitMethodNotFound(final ParseTree tree) {
        throw new RuntimeException(tree.getClass().getSimpleName() + " cannot be converted to action");
    }

    public Void visitValueNode(final ValueNode n) {
        mv.visitLdcInsn(n.toObject());
        // Make sure the item on stack is an object, not a primitive
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
        return null;
    }

    public Void visitUnaryRule(final UnaryRule n) {
        switch (n.op.type) {
            case S_QM:
                mv.visitVarInsn(ALOAD, 1);
                visit(n.rule);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;", false);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                return null;
            default:
                throw new RuntimeException("Unknown unary operator " + n.op);
        }
    }

    public Void visitBinaryRule(final BinaryRule n) {
        switch (n.op.type) {
            case S_LB: {
                // (a, b, c) { ?_it }
                final Label loop = new Label();
                final Label exit = new Label();
                final int original = ++locals;
                visit(n.rule1);
                mv.visitInsn(DUP);
                mv.visitVarInsn(ASTORE, original);
                ASMUtils.testIf(mv, IFNULL, () -> {
                    mv.visitVarInsn(ALOAD, original);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "isArray", "()Z", false);
                    ASMUtils.testIfElse(mv, IFEQ, () -> {
                        mv.visitVarInsn(ALOAD, original);
                        mv.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
                        mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", false);
                    }, () -> {
                        mv.visitVarInsn(ALOAD, original);
                        mv.visitInsn(DUP);
                        mv.visitTypeInsn(INSTANCEOF, "java/util/Map");
                        ASMUtils.testIfElse(mv, IFEQ, () -> {
                            mv.visitTypeInsn(CHECKCAST, "java/util/Map");
                            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "entrySet", "()Ljava/util/Set;", true);
                        }, () -> mv.visitTypeInsn(CHECKCAST, "java/lang/Iterable"));
                    });

                    // At this point, the data on stack is an Iterable
                    final int local = ++locals;
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/Iterable", "iterator", "()Ljava/util/Iterator;", true);
                    mv.visitVarInsn(ASTORE, local);
                    mv.visitLabel(loop);
                    mv.visitVarInsn(ALOAD, local);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
                    mv.visitJumpInsn(IFEQ, exit);
                    mv.visitVarInsn(ALOAD, 1); // store looping value as _it
                    mv.visitLdcInsn("_it");
                    mv.visitVarInsn(ALOAD, local);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                    mv.visitInsn(POP);
                    visit(n.rule2);
                    mv.visitInsn(POP);
                    mv.visitJumpInsn(GOTO, loop);
                    mv.visitLabel(exit);
                    --locals;
                });
                mv.visitVarInsn(ALOAD, original);
                --locals;
                return null;
            }
            case S_AD:
                visit(n.rule1);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                visit(n.rule2);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                mv.visitInsn(DADD);
                break;
            case S_MN:
                visit(n.rule1);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                visit(n.rule2);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                mv.visitInsn(DSUB);
                break;
            case S_ST:
                visit(n.rule1);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                visit(n.rule2);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                mv.visitInsn(DMUL);
                break;
            case S_DV:
                visit(n.rule1);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                visit(n.rule2);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                mv.visitInsn(DDIV);
                break;
            case S_MD:
                visit(n.rule1);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                visit(n.rule2);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                mv.visitInsn(DREM);
                break;
            default:
                throw new RuntimeException("Unknown binary operator " + n.op);
        }

        if (n.op.type.isNumberOp()) {
            // Box value again
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        }
        return null;
    }

    public Void visitKaryRule(final KaryRule n) {
        switch (n.type) {
            case SUBSCRIPT: {
                boolean flag = false;
                for (int i = 0; i < n.rules.size(); ++i) {
                    visit(n.rules.get(i));
                    if (!flag) {
                        flag = true;
                    } else {
                        mv.visitMethodInsn(INVOKESTATIC, "com/ymcmp/rset/lib/Stdlib", "subscript", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                    }
                }
                return null;
            }
            case JOIN:
                mv.visitLdcInsn(n.rules.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                for (int i = 0; i < n.rules.size(); ++i) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(i);
                    visit(n.rules.get(i));
                    mv.visitInsn(AASTORE);
                }
                mv.visitMethodInsn(INVOKESTATIC, "com/ymcmp/rset/lib/Stdlib", "concat", "([Ljava/lang/Object;)Ljava/lang/String;", false);
                return null;
            case ARRAY:
                mv.visitLdcInsn(n.rules.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                for (int i = 0; i < n.rules.size(); ++i) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(i);
                    visit(n.rules.get(i));
                    mv.visitInsn(AASTORE);
                }
                return null;
            case CALL:
                visit(n.rules.get(0));
                mv.visitTypeInsn(CHECKCAST, "java/util/function/Function");
                mv.visitLdcInsn(n.rules.size() - 1);
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                for (int i = 1; i < n.rules.size(); ++i) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(i - 1);
                    visit(n.rules.get(i));
                    mv.visitInsn(AASTORE);
                }
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/function/Function", "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                return null;
            case AND: {
                final Label exit = new Label();
                int i = 0;
                for ( ; i < n.rules.size() - 1; ++i) {
                    visit(n.rules.get(i));
                    mv.visitInsn(DUP);
                    mv.visitMethodInsn(INVOKESTATIC, "com/ymcmp/rset/lib/Stdlib", "isTruthy", "(Ljava/lang/Object;)Z", false);
                    mv.visitJumpInsn(IFEQ, exit);
                    mv.visitInsn(POP);
                }
                // Same logic as OR??? (It works though...)
                visit(n.rules.get(i));
                mv.visitLabel(exit);
                return null;
            }
            case OR: {
                final Label exit = new Label();
                int i = 0;
                for ( ; i < n.rules.size() - 1; ++i) {
                    visit(n.rules.get(i));
                    mv.visitInsn(DUP);
                    mv.visitMethodInsn(INVOKESTATIC, "com/ymcmp/rset/lib/Stdlib", "isTruthy", "(Ljava/lang/Object;)Z", false);
                    mv.visitJumpInsn(IFNE, exit);
                    mv.visitInsn(POP);
                }
                // The last expression must be returned if all previous ones are false...
                visit(n.rules.get(i));
                mv.visitLabel(exit);
                return null;
            }
            case ASSIGN: {
                // NOTE: This implementation does not mutate maps or arrays
                final int k = n.rules.size() - 1;
                final int local = ++locals;
                visit(n.rules.get(k));
                mv.visitVarInsn(ASTORE, local);
                // a = b = c = d --> a, b, c will be d
                for (int i = k - 1; i >= 0; --i) {
                    mv.visitVarInsn(ALOAD, 1);
                    visit(n.rules.get(i));
                    mv.visitVarInsn(ALOAD, local);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                    mv.visitInsn(POP);
                }
                mv.visitVarInsn(ALOAD, local);
                --locals;
                return null;
            }
            case IGNORE: {
                int i = 0;
                for (; i < n.rules.size() - 1; ++i) {
                    visit(n.rules.get(i));
                    mv.visitInsn(POP);
                }
                // Last expression keeps it's value
                visit(n.rules.get(i));
                return null;
            }
            default:
                throw new RuntimeException("Unknown rule block " + n.type);
        }
    }

    public Void visitRulesetNode(final RulesetNode n) {
        final String actnName = "act" + n.name.getText();
        mv = cw.visitMethod(ACC_PUBLIC, actnName, "(Ljava/util/Map;)Ljava/lang/Object;", "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)Ljava/lang/Object;", null);
        mv.visitCode();
        if (n.expr == null) {
            mv.visitInsn(ACONST_NULL);
        } else {
            locals = 1;
            visit(n.expr);
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return null;
    }
}