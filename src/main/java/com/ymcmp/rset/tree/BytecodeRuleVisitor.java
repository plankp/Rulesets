package com.ymcmp.rset.tree;

import java.util.Map;
import java.util.Stack;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;

import org.objectweb.asm.Label;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import com.ymcmp.rset.Type;

import com.ymcmp.lexparse.tree.Visitor;
import com.ymcmp.lexparse.tree.ParseTree;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeRuleVisitor extends Visitor<Void> {

    public enum VarType {
        _COUNTER_RESV, HIDDEN, MAP, LIST, COUNTER, NUM, BOOL;
    }

    private final Stack<VarType> locals = new Stack<>();

    private final Map<String, Consumer<BytecodeRuleVisitor>> refs;
    private final String className;
    private final ClassWriter cw;

    public MethodVisitor mv;

    public int RESULT;

    public BytecodeRuleVisitor(ClassWriter cw, String className, Map<String, Consumer<BytecodeRuleVisitor>> refs) {
        this.cw = cw;
        this.className = className;
        this.refs = refs;
    }

    public int pushNewLocal(VarType t) {
        locals.push(t);
        final int k = locals.size() - 1;
        if (t == VarType.COUNTER) {
            locals.push(VarType._COUNTER_RESV);
        }
        return k;
    }

    public void popLocal() {
        if (locals.pop() == VarType._COUNTER_RESV) {
            locals.pop();
        }
    }

    public int findNearestLocal(VarType t) {
        return locals.lastIndexOf(t);
    }

    public Void visitMethodNotFound(final ParseTree tree) {
        throw new RuntimeException(tree.getClass().getSimpleName() + " cannot be converted to rule");
    }

    public Void visitRefRule(final RefRule n) {
        refs.get(n.node.getText()).accept(this);
        return null;
    }

    public Void visitValueNode(final ValueNode n) {
        switch (n.token.type) {
            case S_ST: {
                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitVarInsn(ALOAD, plst);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "testSlotOccupied", "(Ljava/util/Collection;)Z", false);
                mv.visitVarInsn(ISTORE, RESULT);
                return null;
            }
            case S_EX: {
                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitVarInsn(ALOAD, plst);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "testEnd", "(Ljava/util/Collection;)Z", false);
                mv.visitVarInsn(ISTORE, RESULT);
                return null;
            }
            default: {
                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitLdcInsn(n.toObject());
                if (n.token.type == Type.L_NUMBER) {
                    // Box the integer
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                }
                mv.visitVarInsn(ALOAD, plst);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "testEquality", "(Ljava/lang/Object;Ljava/util/Collection;)Z", false);
                mv.visitVarInsn(ISTORE, RESULT);
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
                mv.visitVarInsn(ILOAD, RESULT);
                mv.visitJumpInsn(IFNE, br0);    // result == true(1)
                mv.visitInsn(ICONST_1);
                mv.visitJumpInsn(GOTO, br1);
                mv.visitLabel(br0);
                mv.visitInsn(ICONST_0);
                mv.visitLabel(br1);
                mv.visitVarInsn(ISTORE, RESULT);
                return null;
            }
            case S_QM: {
                final Label label = new Label();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "save", "()V", false);
                visit(n.rule);
                mv.visitVarInsn(ILOAD, RESULT);
                mv.visitJumpInsn(IFNE, label);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "unsave", "()V", false);
                mv.visitLabel(label);
                mv.visitInsn(ICONST_1);
                mv.visitVarInsn(ISTORE, RESULT);
                return null;
            }
            case S_AD: {
                final Label loop = new Label();
                final Label exit = new Label();
                final int plst = findNearestLocal(VarType.LIST);
                final int flag = pushNewLocal(VarType.BOOL);
                final int list = pushNewLocal(VarType.LIST);
                final int rwnd = pushNewLocal(VarType.NUM);
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, flag);
                mv.visitTypeInsn(NEW, "java/util/ArrayList");
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
                mv.visitVarInsn(ASTORE, list);
                mv.visitLabel(loop);
                mv.visitVarInsn(ALOAD, list);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
                mv.visitVarInsn(ISTORE, rwnd);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "save", "()V", false);
                visit(n.rule);
                mv.visitVarInsn(ILOAD, RESULT);
                mv.visitJumpInsn(IFEQ, exit);
                mv.visitInsn(ICONST_1);
                mv.visitVarInsn(ISTORE, flag);
                mv.visitJumpInsn(GOTO, loop);
                mv.visitLabel(exit);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "unsave", "()V", false);
                mv.visitVarInsn(ALOAD, list);
                mv.visitVarInsn(ILOAD, rwnd);
                mv.visitVarInsn(ALOAD, list);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "subList", "(II)Ljava/util/List;", true);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "clear", "()V", true);
                mv.visitVarInsn(ALOAD, plst);
                mv.visitVarInsn(ALOAD, list);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                mv.visitInsn(POP);
                mv.visitVarInsn(ILOAD, flag);
                mv.visitVarInsn(ISTORE, RESULT);
                popLocal();
                popLocal();
                popLocal();
                return null;
            }
            case S_ST: {
                final Label loop = new Label();
                final Label exit = new Label();
                final int plst = findNearestLocal(VarType.LIST);
                final int list = pushNewLocal(VarType.LIST);
                final int rwnd = pushNewLocal(VarType.NUM);
                mv.visitTypeInsn(NEW, "java/util/ArrayList");
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
                mv.visitVarInsn(ASTORE, list);
                mv.visitLabel(loop);
                mv.visitVarInsn(ALOAD, list);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
                mv.visitVarInsn(ISTORE, rwnd);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "save", "()V", false);
                visit(n.rule);
                mv.visitVarInsn(ILOAD, RESULT);
                mv.visitJumpInsn(IFEQ, exit);
                mv.visitJumpInsn(GOTO, loop);
                mv.visitLabel(exit);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "unsave", "()V", false);
                mv.visitVarInsn(ALOAD, list);
                mv.visitVarInsn(ILOAD, rwnd);
                mv.visitVarInsn(ALOAD, list);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "subList", "(II)Ljava/util/List;", true);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "clear", "()V", true);
                mv.visitVarInsn(ALOAD, plst);
                mv.visitVarInsn(ALOAD, list);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                mv.visitInsn(POP);
                mv.visitInsn(ICONST_1);
                mv.visitVarInsn(ISTORE, RESULT);
                popLocal();
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
                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitLdcInsn(((ValueNode) n.rule1).toObject());
                mv.visitLdcInsn(((ValueNode) n.rule2).toObject());
                mv.visitVarInsn(ALOAD, plst);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "testRange", "(Ljava/lang/Comparable;Ljava/lang/Comparable;Ljava/util/Collection;)Z", false);
                mv.visitVarInsn(ISTORE, RESULT);
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
                final int out = findNearestLocal(VarType.LIST);
                final int lst = pushNewLocal(VarType.LIST);
                mv.visitTypeInsn(NEW, "java/util/ArrayList");
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
                mv.visitVarInsn(ASTORE, lst);
                n.rules.forEach(k -> {
                    visit(k);
                    mv.visitVarInsn(ILOAD, RESULT);
                    mv.visitJumpInsn(IFEQ, exit);
                });
                mv.visitInsn(ICONST_1);
                mv.visitVarInsn(ISTORE, RESULT);
                mv.visitLabel(exit);
                mv.visitVarInsn(ALOAD, out);
                mv.visitVarInsn(ALOAD, lst);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                mv.visitInsn(POP);
                popLocal();
                return null;
            }
            case SWITCH: {
                final Label exit = new Label();
                final int list = findNearestLocal(VarType.LIST);
                final int rwnd = pushNewLocal(VarType.NUM);
                n.rules.forEach(k -> {
                    mv.visitVarInsn(ALOAD, list);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
                    mv.visitVarInsn(ISTORE, rwnd);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "save", "()V", false);
                    visit(k);
                    mv.visitVarInsn(ILOAD, RESULT);
                    mv.visitJumpInsn(IFNE, exit);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "unsave", "()V", false);
                    mv.visitVarInsn(ALOAD, list);
                    mv.visitVarInsn(ILOAD, rwnd);
                    mv.visitVarInsn(ALOAD, list);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "subList", "(II)Ljava/util/List;", true);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "clear", "()V", true);
                });
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, RESULT);
                mv.visitLabel(exit);
                popLocal();
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
        final Label br2 = new Label();
        // final int lst = pushNewLocal(VarType.LIST);
        final int plst = findNearestLocal(VarType.LIST);
        final int map = findNearestLocal(VarType.MAP);
        // mv.visitTypeInsn(NEW, "java/util/ArrayList");
        // mv.visitInsn(DUP);
        // mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        // mv.visitVarInsn(ASTORE, lst);
        visit(n.rule);
        mv.visitVarInsn(ALOAD, map);    // Setting up stack for map.put
        mv.visitLdcInsn(dest);          // Setting up stack for map.put(dest
        mv.visitVarInsn(ILOAD, RESULT);
        mv.visitJumpInsn(IFEQ, br0);

        mv.visitVarInsn(ALOAD, plst);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ISUB);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);

        // mv.visitInsn(DUP);
        // mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
        // mv.visitInsn(ICONST_1);
        // mv.visitJumpInsn(IF_ICMPNE, br2); // if size != 1
        // mv.visitInsn(ICONST_0);
        // mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
        // mv.visitLabel(br2);

        mv.visitJumpInsn(GOTO, br1);
        mv.visitLabel(br0);
        mv.visitInsn(ICONST_0);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitLabel(br1);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitInsn(POP);
        // popLocal();
        return null;
    }

    public Void visitRulesetNode(final RulesetNode n) {
        final String testName = "test" + n.name.getText();
        mv = cw.visitMethod(ACC_PUBLIC, testName, "(Ljava/util/Map;Ljava/util/List;)Z", "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;Ljava/util/List<Ljava/lang/Object;>;)Z", null);
        mv.visitCode();

        pushNewLocal(VarType.HIDDEN);  // this
        final int env = pushNewLocal(VarType.MAP);      // env
        final int lst = pushNewLocal(VarType.LIST);     // lst
        RESULT = pushNewLocal(VarType.BOOL);     // results
        final int map = pushNewLocal(VarType.MAP);

        // Initialize objects
        mv.visitTypeInsn(NEW, "java/util/HashMap");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, map);
        // mv.visitTypeInsn(NEW, "java/util/ArrayList");
        // mv.visitInsn(DUP);
        // mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        // mv.visitVarInsn(ASTORE, lst);

        // Fill in the actual clauses here!
        visit(n.rule);
        // if (env@1 != null) %additional
        mv.visitVarInsn(ALOAD, env);
        final Label label = new Label();
        mv.visitJumpInsn(IFNULL, label);
        // %additional -> env@1.putAll(captures@3)
        mv.visitVarInsn(ALOAD, env);
        mv.visitVarInsn(ALOAD, map);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "putAll", "(Ljava/util/Map;)V", true);
        mv.visitLabel(label);
        // return result@2
        mv.visitVarInsn(ILOAD, RESULT);
        mv.visitInsn(IRETURN);

        popLocal();
        popLocal();
        popLocal();
        popLocal();
        popLocal();

        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return null;
    }
}