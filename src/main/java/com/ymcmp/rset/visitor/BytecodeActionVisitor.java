/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.visitor;

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import com.ymcmp.rset.tree.*;

import com.ymcmp.lexparse.tree.ParseTree;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeActionVisitor extends BaseVisitor {

    private ClassWriter cw;
    private String className;
    private MethodVisitor mv;
    private int locals;

    public BytecodeActionVisitor(ClassWriter cw, String className) {
        this.cw = cw;
        this.className = className;
    }

    @Override
    public MethodVisitor getMethodVisitor() {
        return this.mv;
    }

    public void visitValueNode(final ValueNode n) {
        pushAsObject(n);
    }

    public void visitUnaryRule(final UnaryRule n) {
        switch (n.op.type) {
            case S_QM:
                mv.visitVarInsn(ALOAD, 1);
                visit(n.rule);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;", false);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                break;
            default:
                throw new RuntimeException("Unknown unary operator " + n.op);
        }
    }

    private void castRuleToDouble(ParseTree rule) {
        visit(rule);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
    }

    private void binaryDoubleOp(ParseTree lhs, ParseTree rhs, int op) {
        castRuleToDouble(lhs);
        castRuleToDouble(rhs);
        mv.visitInsn(op);
        doubleToNumber();
    }

    private void callMathlibCompareMethod(ParseTree lhs, ParseTree rhs, String method) {
        visit(lhs);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Comparable");
        visit(rhs);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Comparable");
        mv.visitMethodInsn(INVOKESTATIC, "com/ymcmp/rset/lib/Mathlib", method, "(Ljava/lang/Comparable;Ljava/lang/Comparable;)Z", false);
        // Box the boolean value
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
    }

    private void doubleToNumber() {
        // Cast to int if value is same, then box value
        mv.visitInsn(DUP2);
        mv.visitInsn(DUP2);
        mv.visitInsn(D2I);
        mv.visitInsn(I2D);
        mv.visitInsn(DCMPL);
        testIfElse(IFEQ, () -> {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        }, () -> {
            mv.visitInsn(D2I);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        });
    }

    public void visitBinaryRule(final BinaryRule n) {
        switch (n.op.type) {
            case S_LB:
                // (a, b, c) { ?_it }
                visit(n.rule1);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESTATIC, "com/ymcmp/rset/lib/Arraylib", "toIterable", "(Ljava/lang/Object;)Ljava/lang/Iterable;", false);
                mv.visitInsn(DUP);
                testIfElse(IFNULL, () -> {
                    // At this point, the data on stack is an Iterable
                    final int local = ++locals;
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/Iterable", "iterator", "()Ljava/util/Iterator;", true);
                    mv.visitVarInsn(ASTORE, local);
                    whileLoop(exit -> {
                        mv.visitVarInsn(ALOAD, local);
                        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
                        mv.visitJumpInsn(IFEQ, exit);
                    }, (exit, loop) -> {
                        mv.visitVarInsn(ALOAD, 1); // store looping value as _it
                        mv.visitLdcInsn("_it");
                        mv.visitVarInsn(ALOAD, local);
                        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
                        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                        visit(n.rule2);
                        mv.visitInsn(POP2);
                    });
                    --locals;
                }, () -> mv.visitInsn(POP));
                break;
            case S_AD:
                binaryDoubleOp(n.rule1, n.rule2, DADD);
                break;
            case S_MN:
                binaryDoubleOp(n.rule1, n.rule2, DSUB);
                break;
            case S_ST:
                binaryDoubleOp(n.rule1, n.rule2, DMUL);
                break;
            case S_DV:
                binaryDoubleOp(n.rule1, n.rule2, DDIV);
                break;
            case S_MD:
                binaryDoubleOp(n.rule1, n.rule2, DREM);
                break;
            case S_EX:
                visit(n.rule1);
                visit(n.rule2);
                mv.visitMethodInsn(INVOKESTATIC, "com/ymcmp/rset/lib/Reflectlib", "access", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                break;
            case S_LA:
                callMathlibCompareMethod(n.rule1, n.rule2, "_lt");
                break;
            case S_RA:
                callMathlibCompareMethod(n.rule1, n.rule2, "_gt");
                break;
            default:
                throw new RuntimeException("Unknown binary operator " + n.op);
        }
    }

    private void generateShortCircuitRoutine(int branchOp, List<ParseTree> body) {
        final Label exit = new Label();
        int i = 0;
        for ( ; i < body.size() - 1; ++i) {
            visit(body.get(i));
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESTATIC, "com/ymcmp/rset/lib/Stdlib", "isTruthy", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(branchOp, exit);
            mv.visitInsn(POP);
        }
        // The last expression must be returned if all previous ones are false...
        visit(body.get(i));
        mv.visitLabel(exit);
    }

    private void storeToArray(List<ParseTree> exprs) {
        mv.visitLdcInsn(exprs.size());
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < exprs.size(); ++i) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            visit(exprs.get(i));
            mv.visitInsn(AASTORE);
        }
    }

    @Override
    public void visitRuleSubscript(final List<ParseTree> rules) {
        boolean flag = false;
        for (int i = 0; i < rules.size(); ++i) {
            visit(rules.get(i));
            if (!flag) {
                flag = true;
            } else {
                mv.visitMethodInsn(INVOKESTATIC, "com/ymcmp/rset/lib/Arraylib", "subscript", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
            }
        }
    }

    @Override
    public void visitRuleJoin(final List<ParseTree> rules) {
        storeToArray(rules);
        mv.visitMethodInsn(INVOKESTATIC, "com/ymcmp/rset/lib/Stdlib", "concat", "([Ljava/lang/Object;)Ljava/lang/String;", false);
    }

    @Override
    public void visitRuleArray(final List<ParseTree> rules) {
        storeToArray(rules);
    }

    @Override
    public void visitRuleCall(final List<ParseTree> rules) {
        visit(rules.get(0));
        mv.visitTypeInsn(CHECKCAST, "java/util/function/Function");
        storeToArray(rules.subList(1, rules.size()));
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/function/Function", "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
    }

    @Override
    public void visitRuleAnd(final List<ParseTree> rules) {
        generateShortCircuitRoutine(IFEQ, rules);
    }

    @Override
    public void visitRuleOr(final List<ParseTree> rules) {
        generateShortCircuitRoutine(IFNE, rules);
    }

    @Override
    public void visitRuleAssign(final List<ParseTree> rules) {
        // NOTE: This implementation does not mutate maps or arrays
        final int k = rules.size() - 1;
        final int local = ++locals;
        visit(rules.get(k));
        mv.visitVarInsn(ASTORE, local);
        // a = b = c = d --> a, b, c will be d
        for (int i = k - 1; i >= 0; --i) {
            mv.visitVarInsn(ALOAD, 1);
            visit(rules.get(i));
            mv.visitVarInsn(ALOAD, local);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);
        }
        mv.visitVarInsn(ALOAD, local);
        --locals;
    }

    @Override
    public void visitRuleIgnore(final List<ParseTree> rules) {
        int i = 0;
        for (; i < rules.size() - 1; ++i) {
            visit(rules.get(i));
            mv.visitInsn(POP);
        }
        // Last expression keeps it's value
        visit(rules.get(i));
    }

    public void visitRulesetNode(final RulesetNode n) {
        final String actnName = n.makeActnName().get();
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
    }
}