/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

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

    public final boolean genDebugInfo;

    public MethodVisitor mv;

    public int RESULT;

    public BytecodeRuleVisitor(ClassWriter cw, String className, boolean genDebugInfo, Map<String, Consumer<BytecodeRuleVisitor>> refs) {
        this.cw = cw;
        this.className = className;
        this.genDebugInfo = genDebugInfo;
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
                if (genDebugInfo) {
                    mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                    mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
                    mv.visitLdcInsn("Test wildcard slot");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                }

                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitVarInsn(ALOAD, plst);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "testSlotOccupied", "(Ljava/util/Collection;)Z", false);
                mv.visitVarInsn(ISTORE, RESULT);
                return null;
            }
            case S_EX: {
                if (genDebugInfo) {
                    mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                    mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
                    mv.visitLdcInsn("Test end of data");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                }

                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitVarInsn(ALOAD, plst);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "testEnd", "(Ljava/util/Collection;)Z", false);
                mv.visitVarInsn(ISTORE, RESULT);
                return null;
            }
            default: {
                if (genDebugInfo) {
                    mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                    mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
                    mv.visitLdcInsn("Test for " + n.getText());
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                }

                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitLdcInsn(n.toObject());
                // Make sure the item on stack is an object, not a primitive
                switch (n.token.type) {
                    case L_INT:
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                        break;
                    case L_REAL:
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                        break;
                    case L_CHARS: {
                        // convert to char[], use a different testEquality...
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false);

                        final Label loop = new Label();
                        final Label exit = new Label();
                        final int len = n.toObject().toString().length(); // Strings are immutable, compute length at compile time
                        final int lst = pushNewLocal(VarType.LIST);
                        final int arr = pushNewLocal(VarType.LIST);
                        final int idx = pushNewLocal(VarType.NUM);
                        newObjectNoArgs(lst, "java/util/ArrayList");
                        mv.visitVarInsn(ASTORE, arr);
                        mv.visitInsn(ICONST_0);
                        mv.visitVarInsn(ISTORE, idx);
                        mv.visitInsn(ICONST_0);
                        mv.visitVarInsn(ISTORE, RESULT);
                        mv.visitLabel(loop);
                        mv.visitVarInsn(ILOAD, idx);
                        mv.visitLdcInsn(len);
                        mv.visitJumpInsn(IF_ICMPGE, exit);
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                        mv.visitVarInsn(ALOAD, arr);
                        mv.visitVarInsn(ILOAD, idx);
                        mv.visitInsn(CALOAD);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                        mv.visitVarInsn(ALOAD, lst);
                        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "testEquality", "(Ljava/lang/Object;Ljava/util/Collection;)Z", false);
                        mv.visitInsn(DUP);
                        mv.visitVarInsn(ISTORE, RESULT);
                        mv.visitJumpInsn(IFEQ, exit);
                        mv.visitIincInsn(idx, 1);
                        mv.visitJumpInsn(GOTO, loop);
                        mv.visitLabel(exit);
                        mv.visitInsn(POP);
                        mv.visitVarInsn(ALOAD, plst);
                        mv.visitVarInsn(ALOAD, lst);
                        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                        mv.visitInsn(POP);
                        popLocal();
                        popLocal();
                        popLocal();
                        break;
                    }
                }

                if (n.token.type != Type.L_CHARS) {
                    mv.visitVarInsn(ALOAD, plst);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "testEquality", "(Ljava/lang/Object;Ljava/util/Collection;)Z", false);
                    mv.visitVarInsn(ISTORE, RESULT);
                }
                return null;
            }
        }
    }

    private void newObjectNoArgs(int slot, String className) {
        ASMUtils.newObjectNoArgs(mv, className, slot);
    }

    private void saveStack(int listSlot, int rewindSlot) {
        mv.visitVarInsn(ALOAD, listSlot);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
        mv.visitVarInsn(ISTORE, rewindSlot);
    }

    private void saveRoutine(int listSlot, int rewindSlot) {
        if (genDebugInfo) {
            mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
            mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINER", "Ljava/util/logging/Level;");
            mv.visitLdcInsn("Save parse stack");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
        }

        saveStack(listSlot, rewindSlot);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "save", "()V", false);
    }

    private void unsaveStack(int listSlot, int rewindSlot) {
        mv.visitVarInsn(ALOAD, listSlot);
        mv.visitVarInsn(ILOAD, rewindSlot);
        mv.visitVarInsn(ALOAD, listSlot);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "subList", "(II)Ljava/util/List;", true);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "clear", "()V", true);
    }

    private void unsaveRoutine(int listSlot, int rewindSlot) {
        if (genDebugInfo) {
            mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
            mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINER", "Ljava/util/logging/Level;");
            mv.visitLdcInsn("Restore parse stack");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
        }

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "unsave", "()V", false);
        unsaveStack(listSlot, rewindSlot);
    }

    public Void visitUnaryRule(final UnaryRule n) {
        switch (n.op.type) {
            case S_TD: {
                if (genDebugInfo) {
                    mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                    mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
                    mv.visitLdcInsn("Negate next clause");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                }

                visit(n.rule);
                mv.visitVarInsn(ILOAD, RESULT);
                ASMUtils.testIfElse(mv, IFNE, () -> mv.visitInsn(ICONST_1), () -> mv.visitInsn(ICONST_0));
                mv.visitVarInsn(ISTORE, RESULT);
                return null;
            }
            case S_QM: {
                if (genDebugInfo) {
                    mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                    mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
                    mv.visitLdcInsn("[0, 1] next clause");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                }

                final Label label = new Label();
                final int list = findNearestLocal(VarType.LIST);
                final int rwnd = pushNewLocal(VarType.NUM);
                saveRoutine(list, rwnd);
                visit(n.rule);
                mv.visitVarInsn(ILOAD, RESULT);
                ASMUtils.testIf(mv, IFNE, () -> unsaveRoutine(list, rwnd));
                mv.visitInsn(ICONST_1);
                mv.visitVarInsn(ISTORE, RESULT);
                popLocal();
                return null;
            }
            case S_AD: {
                if (genDebugInfo) {
                    mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                    mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
                    mv.visitLdcInsn("[1, n] next clause");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                }

                final Label loop = new Label();
                final int plst = findNearestLocal(VarType.LIST);
                final int flag = pushNewLocal(VarType.BOOL);
                final int list = pushNewLocal(VarType.LIST);
                final int rwnd = pushNewLocal(VarType.NUM);
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, flag);
                newObjectNoArgs(list, "java/util/ArrayList");
                mv.visitLabel(loop);
                saveRoutine(list, rwnd);
                visit(n.rule);
                mv.visitVarInsn(ILOAD, RESULT);
                ASMUtils.testIf(mv, IFEQ, () -> {
                    mv.visitInsn(ICONST_1);
                    mv.visitVarInsn(ISTORE, flag);
                    mv.visitJumpInsn(GOTO, loop);
                });
                unsaveRoutine(list, rwnd);
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
                if (genDebugInfo) {
                    mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                    mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
                    mv.visitLdcInsn("[0, n] next clause");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                }

                final Label loop = new Label();
                final int plst = findNearestLocal(VarType.LIST);
                final int list = pushNewLocal(VarType.LIST);
                final int rwnd = pushNewLocal(VarType.NUM);
                newObjectNoArgs(list, "java/util/ArrayList");
                mv.visitLabel(loop);
                saveRoutine(list, rwnd);
                visit(n.rule);
                mv.visitVarInsn(ILOAD, RESULT);
                ASMUtils.testIf(mv, IFEQ, () -> mv.visitJumpInsn(GOTO, loop));
                unsaveRoutine(list, rwnd);
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
                final ValueNode node1 = (ValueNode) n.rule1;
                final ValueNode node2 = (ValueNode) n.rule2;
                if (genDebugInfo) {
                    mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                    mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
                    mv.visitLdcInsn("Test range of [" + node1.getText() + ", " + node2.getText() + "]");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                }

                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitLdcInsn(node1.toObject());
                mv.visitLdcInsn(node2.toObject());
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
                final int ruleCount = n.rules.size();
                if (genDebugInfo) {
                    mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                    mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
                    mv.visitLdcInsn("Sequential clauses (" + ruleCount + " total):");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                }

                final Label exit = new Label();
                final int out = findNearestLocal(VarType.LIST);
                final int lst = pushNewLocal(VarType.LIST);
                newObjectNoArgs(lst, "java/util/ArrayList");
                for (int i = 0; i < ruleCount; ++i) {
                    if (genDebugInfo) {
                        mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                        mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINER", "Ljava/util/logging/Level;");
                        mv.visitLdcInsn("Sequential clause " + (i + 1) + " out of " + ruleCount + ":");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                    }

                    visit(n.rules.get(i));
                    mv.visitVarInsn(ILOAD, RESULT);
                    mv.visitJumpInsn(IFEQ, exit);
                }
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
                final int ruleCount = n.rules.size();
                if (genDebugInfo) {
                    mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                    mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
                    mv.visitLdcInsn("Switch clauses (" + ruleCount + " total):");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                }

                final Label exit = new Label();
                final int list = findNearestLocal(VarType.LIST);
                final int rwnd = pushNewLocal(VarType.NUM);
                for (int i = 0; i < ruleCount; ++i) {
                    if (genDebugInfo) {
                        mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                        mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINER", "Ljava/util/logging/Level;");
                        mv.visitLdcInsn("Switch clause " + (i + 1) + " out of " + ruleCount + ":");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                    }

                    saveRoutine(list, rwnd);
                    visit(n.rules.get(i));
                    mv.visitVarInsn(ILOAD, RESULT);
                    mv.visitJumpInsn(IFNE, exit);
                    unsaveRoutine(list, rwnd);
                };
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, RESULT);
                mv.visitLabel(exit);
                popLocal();
                return null;
            }
            case GROUP: {
                final int ruleCount = n.rules.size();
                if (genDebugInfo) {
                    mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                    mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
                    mv.visitLdcInsn("Grouping clauses (" + ruleCount + " total):");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                }

                final Label br0 = new Label();
                final Label br1 = new Label();
                final int plst = findNearestLocal(VarType.LIST);
                final int list = pushNewLocal(VarType.LIST);
                final int rwnd = pushNewLocal(VarType.NUM);
                newObjectNoArgs(list, "java/util/ArrayList");
                saveStack(plst, rwnd);
                for (int i = 0; i < ruleCount; ++i) {
                    if (genDebugInfo) {
                        mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
                        mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINER", "Ljava/util/logging/Level;");
                        mv.visitLdcInsn("Group clause " + (i + 1) + " out of " + ruleCount + ":");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
                    }

                    visit(n.rules.get(i));
                    mv.visitVarInsn(ILOAD, RESULT);
                    mv.visitJumpInsn(IFEQ, br0);
                    mv.visitVarInsn(ALOAD, plst);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(ISUB);
                    mv.visitInsn(DUP);
                    mv.visitInsn(ICONST_0);
                    ASMUtils.testIf(mv, IF_ICMPLT, () -> {
                        mv.visitVarInsn(ALOAD, list);
                        mv.visitInsn(SWAP);
                        mv.visitVarInsn(ALOAD, plst);
                        mv.visitInsn(SWAP);
                        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "remove", "(I)Ljava/lang/Object;", true);
                        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                    });
                    mv.visitInsn(POP);
                }
                mv.visitVarInsn(ALOAD, plst);
                mv.visitVarInsn(ALOAD, list);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                mv.visitInsn(POP);
                mv.visitJumpInsn(GOTO, br1);
                mv.visitLabel(br0);
                unsaveStack(plst, rwnd);
                mv.visitLabel(br1);
                popLocal();
                popLocal();
                return null;
            }
            default:
                throw new RuntimeException("Unknown rule block " + n.type);
        }
    }

    public Void visitCaptureRule(final CaptureRule n) {
        final Label br0 = new Label();
        final Label br1 = new Label();
        final Label br2 = new Label();
        final String dest = n.dest.getText();
        final int plst = findNearestLocal(VarType.LIST);
        final int map = findNearestLocal(VarType.MAP);
        if (genDebugInfo) {
            mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
            mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINER", "Ljava/util/logging/Level;");
            mv.visitLdcInsn("Capturing next clause as " + dest);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
        }

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
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(IF_ICMPLT, br2);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
        mv.visitJumpInsn(GOTO, br1);
        mv.visitLabel(br2);
        mv.visitInsn(POP2);
        mv.visitLabel(br0);
        mv.visitFieldInsn(GETSTATIC, "java/util/Collections", "EMPTY_LIST", "Ljava/util/List;");
        mv.visitLabel(br1);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitInsn(POP);
        return null;
    }

    public Void visitRulesetNode(final RulesetNode n) {
        final String name = n.name.getText();
        final String testName = "test" + name;
        mv = cw.visitMethod(ACC_PUBLIC, testName, "(Ljava/util/Map;Ljava/util/List;)Z", "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;Ljava/util/List<Ljava/lang/Object;>;)Z", null);
        mv.visitCode();

        pushNewLocal(VarType.HIDDEN);  // this
        final int env = pushNewLocal(VarType.MAP);      // env
        final int lst = pushNewLocal(VarType.LIST);     // lst
        final int map = pushNewLocal(VarType.MAP);
        RESULT = pushNewLocal(VarType.BOOL);

        // Initialize objects
        newObjectNoArgs(map, "java/util/HashMap");

        if (genDebugInfo) {
            mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
            mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
            mv.visitLdcInsn("Entering rule " + name);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
        }

        // Fill in the actual clauses here!
        visit(n.rule);
        // if (env@1 != null) %additional
        mv.visitVarInsn(ALOAD, env);
        ASMUtils.testIf(mv, IFNULL, () -> {
            // %additional -> env@1.putAll(captures@3)
            mv.visitVarInsn(ALOAD, env);
            mv.visitVarInsn(ALOAD, map);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "putAll", "(Ljava/util/Map;)V", true);
        });

        if (genDebugInfo) {
            mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
            mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
            mv.visitLdcInsn("Exiting rule " + name);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
        }

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