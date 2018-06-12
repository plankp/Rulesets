package com.ymcmp.rset.tree;

import java.util.Map;
import java.util.Stack;
import java.util.Arrays;
import java.util.HashMap;

import org.objectweb.asm.Label;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import com.ymcmp.lexparse.tree.Visitor;
import com.ymcmp.lexparse.tree.ParseTree;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeRuleVisitor extends Visitor<Void> {

    private enum Type {
        _COUNTER_RESV, HIDDEN, MAP, COUNTER, NUM, BOOL;
    }

    private final Stack<Type> locals = new Stack<>();

    private ClassWriter cw;
    private String className;
    private MethodVisitor mv;

    public BytecodeRuleVisitor(ClassWriter cw, String className) {
        this.cw = cw;
        this.className = className;
    }

    private int pushNewLocal(Type t) {
        locals.push(t);
        final int k = locals.size() - 1;
        if (t == Type.COUNTER) {
            locals.push(Type._COUNTER_RESV);
        }
        return k;
    }

    public void popLocal() {
        if (locals.pop() == Type._COUNTER_RESV) {
            locals.pop();
        }
    }

    public int findNearestLocal(Type t) {
        return locals.lastIndexOf(t);
    }

    public Void visitMethodNotFound(final ParseTree tree) {
        throw new RuntimeException(tree.getClass().getSimpleName() + " cannot be converted to rule");
    }

    public Void visitRefRule(final RefRule n) {
        final String testName = "test" + n.node.getText();
        final int map = findNearestLocal(Type.MAP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, map);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, testName, "(Ljava/util/Map;)Z", false);
        mv.visitVarInsn(ISTORE, 2);
        return null;
    }

    public Void visitValueNode(final ValueNode n) {
        switch (n.token.type) {
            case S_ST: {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "testSlotOccupied", "()Z", false);
                mv.visitVarInsn(ISTORE, 2);
                return null;
            }
            case S_EX: {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "testEnd", "()Z", false);
                mv.visitVarInsn(ISTORE, 2);
                return null;
            }
            default: {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                mv.visitLdcInsn(n.toObject());
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "testEquality", "(Ljava/lang/Object;)Z", false);
                mv.visitVarInsn(ISTORE, 2);
                return null;
            }
        }
    }

    public Void visitUnaryRule(final UnaryRule n) {
        switch (n.op.type) {
            case S_TD: {
                final Label br0 = new Label();
                final Label br1 = new Label();

                visit(n.rule);
                mv.visitVarInsn(ILOAD, 2);
                mv.visitJumpInsn(IFNE, br0);    // result == true(1)
                mv.visitInsn(ICONST_1);
                mv.visitJumpInsn(GOTO, br1);
                mv.visitLabel(br0);
                mv.visitInsn(ICONST_0);
                mv.visitLabel(br1);
                mv.visitVarInsn(ISTORE, 2);
                return null;
            }
            case S_QM: {
                final Label label = new Label();

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "save", "()V", false);
                visit(n.rule);
                mv.visitVarInsn(ILOAD, 2);
                mv.visitJumpInsn(IFNE, label);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "unsave", "()V", false);
                mv.visitLabel(label);
                mv.visitInsn(ICONST_1);
                mv.visitVarInsn(ISTORE, 2);
                return null;
            }
            case S_AD: {
                final Label loop = new Label();
                final Label exit = new Label();
                final int counter = pushNewLocal(Type.COUNTER);

                // Use this to fix data into captures
                final int merger = findNearestLocal(Type.MAP);

                mv.visitInsn(LCONST_0);
                mv.visitVarInsn(LSTORE, counter);
                mv.visitLabel(loop);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "save", "()V", false);
                visit(n.rule);
                mv.visitVarInsn(ILOAD, 2);
                mv.visitJumpInsn(IFEQ, exit);
                mv.visitVarInsn(LLOAD, counter);
                mv.visitInsn(LCONST_1);
                mv.visitInsn(LADD);
                mv.visitVarInsn(LSTORE, counter);

                // TODO: Capture here

                mv.visitJumpInsn(GOTO, loop);
                mv.visitLabel(exit);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "unsave", "()V", false);
                mv.visitVarInsn(LLOAD, counter);
                mv.visitInsn(LCONST_0);
                mv.visitInsn(LCMP);
                mv.visitVarInsn(ISTORE, 2);
                popLocal();
                return null;
            }
            case S_ST: {
                final Label loop = new Label();
                final Label exit = new Label();
                final int counter = pushNewLocal(Type.COUNTER);

                // Use this to fix data into captures
                final int merger = findNearestLocal(Type.MAP);

                mv.visitInsn(LCONST_0);
                mv.visitVarInsn(LSTORE, counter);
                mv.visitLabel(loop);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "save", "()V", false);
                visit(n.rule);
                mv.visitVarInsn(ILOAD, 2);
                mv.visitJumpInsn(IFEQ, exit);
                mv.visitVarInsn(LLOAD, counter);
                mv.visitInsn(LCONST_1);
                mv.visitInsn(LADD);
                mv.visitVarInsn(LSTORE, counter);

                // TODO: Capture here

                mv.visitJumpInsn(GOTO, loop);
                mv.visitLabel(exit);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "unsave", "()V", false);
                mv.visitInsn(LCONST_1);
                mv.visitVarInsn(ISTORE, 2);
                popLocal();
                return null;
            }
            default:
                throw new RuntimeException("Unknown unary operator " + n.op);
        }
    }

    public Void visitBinaryRule(final BinaryRule n) {
        switch (n.op.type) {
            case S_MN: {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                mv.visitLdcInsn(((ValueNode) n.rule1).toObject());
                mv.visitLdcInsn(((ValueNode) n.rule2).toObject());
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "testRange", "(Ljava/lang/Comparable;Ljava/lang/Comparable;)Z", false);
                mv.visitVarInsn(ISTORE, 2);
                return null;
            }
            default:
                throw new RuntimeException("Unknown binary operator " + n.op);
        }
    }

    public Void visitKaryRule(final KaryRule n) {
        switch (n.type) {
            case SEQ: {
                final Label exit = new Label();
                mv.visitTypeInsn(NEW, "java/util/HashMap");
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
                final int mergerMap = pushNewLocal(Type.MAP);
                mv.visitVarInsn(ASTORE, mergerMap);
                n.rules.forEach(k -> {
                    visit(k);
                    mv.visitVarInsn(ILOAD, 2);
                    mv.visitJumpInsn(IFEQ, exit);
                    mv.visitVarInsn(ALOAD, mergerMap);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "putAll", "(Ljava/util/Map;)V", true);
                });
                mv.visitVarInsn(ALOAD, mergerMap);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "putAll", "(Ljava/util/Map;)V", true);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitVarInsn(ALOAD, mergerMap);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "putAll", "(Ljava/util/Map;)V", true);
                mv.visitInsn(ICONST_1);
                mv.visitVarInsn(ISTORE, 2);
                mv.visitLabel(exit);
                popLocal();
                return null;
            }
            case SWITCH: {
                final Label exit = new Label();
                n.rules.forEach(k -> {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "save", "()V", false);
                    visit(k);
                    mv.visitVarInsn(ILOAD, 2);
                    mv.visitJumpInsn(IFNE, exit);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "unsave", "()V", false);
                });
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, 2);
                mv.visitLabel(exit);
                return null;
            }
            default:
                throw new RuntimeException("Unknown rule block " + n.type);
        }
    }

    public Void visitCaptureRule(final CaptureRule n) {
        final String dest = n.dest.getText();
        final Label br0 = new Label();
        final Label br1 = new Label();
        final int src = pushNewLocal(Type.NUM);
        final int map = findNearestLocal(Type.MAP);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "getIndex", "()I", false);
        mv.visitVarInsn(ISTORE, src);
        visit(n.rule);
        mv.visitVarInsn(ALOAD, map);    // Setting up stack for map.put
        mv.visitLdcInsn(dest);          // Setting up stack for map.put(dest
        mv.visitVarInsn(ILOAD, 2);
        mv.visitJumpInsn(IFEQ, br0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, src);
        mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/EvalState;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/EvalState", "coptFromIndex", "(I)[Ljava/lang/Object;", false);
        mv.visitJumpInsn(GOTO, br1);
        mv.visitLabel(br0);
        mv.visitInsn(ICONST_0);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitLabel(br1);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/util/Object;)Ljava/util/Object;", true);
        mv.visitInsn(POP);
        popLocal();
        return null;
    }

    public Void visitRulesetNode(final RulesetNode n) {
        final String testName = "test" + n.name.getText();
        mv = cw.visitMethod(ACC_PUBLIC, testName, "(Ljava/util/Map;)Z", "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)Z", null);
        mv.visitCode();

        pushNewLocal(Type.HIDDEN);  // this
        pushNewLocal(Type.MAP);     // env
        pushNewLocal(Type.BOOL);    // results

        // captures@3 = new HashMap();
        mv.visitTypeInsn(NEW, "java/util/HashMap");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, pushNewLocal(Type.MAP));

        // Fill in the actual clauses here!
        visit(n.rule);
        // if (env@1 != null) %additional
        mv.visitVarInsn(ALOAD, 1);
        final Label label = new Label();
        mv.visitJumpInsn(IFNULL, label);
        // %additional -> env@1.putAll(captures@3)
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "putAll", "(Ljava/util/Map;)V", true);
        mv.visitLabel(label);
        // return result@2
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(IRETURN);

        popLocal();
        popLocal();
        popLocal();
        popLocal();

        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return null;
    }
}