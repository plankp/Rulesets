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
        _COUNTER_RESV, HIDDEN, MAP, LIST, COUNTER, NUM, BOOL, EVAL_STATE;
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

    public void logMessage(final String level, final String message) {
        if (genDebugInfo) {
            mv.visitFieldInsn(GETSTATIC, className, "LOGGER", "Ljava/util/logging/Logger;");
            mv.visitFieldInsn(GETSTATIC, "java/util/logging/Level", level, "Ljava/util/logging/Level;");
            mv.visitLdcInsn(message);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V", false);
        }
    }

    public Void visitMethodNotFound(final ParseTree tree) {
        throw new RuntimeException(tree.getClass().getSimpleName() + " cannot be converted to rule");
    }

    public Void visitRefRule(final RefRule n) {
        final String name = n.node.getText();
        final Consumer<BytecodeRuleVisitor> cons = refs.get(name);
        if (cons == null) throw new RuntimeException("Attempt to reference undeclared rule " + name);
        cons.accept(this);
        return null;
    }

    public Void visitValueNode(final ValueNode n) {
        switch (n.token.type) {
            case S_ST: {
                logMessage("FINE", "Test wildcard slot");

                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitVarInsn(ALOAD, plst);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "testSlotOccupied", "(Ljava/util/Collection;)Z", false);
                mv.visitVarInsn(ISTORE, RESULT);
                return null;
            }
            case S_EX: {
                logMessage("FINE", "Test end of data");

                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitVarInsn(ALOAD, plst);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "testEnd", "(Ljava/util/Collection;)Z", false);
                mv.visitVarInsn(ISTORE, RESULT);
                return null;
            }
            default: {
                logMessage("FINE", "Test for " + n.getText());

                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");

                final Object obj = n.toObject();
                if (obj == null) {
                    // LdcInsn does not handle nulls
                    mv.visitInsn(ACONST_NULL);
                } else {
                    mv.visitLdcInsn(obj);
                }
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
        logMessage("FINER", "Save parse stack");

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
        logMessage("FINER", "Restore parse stack");

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "unsave", "()V", false);
        unsaveStack(listSlot, rewindSlot);
    }

    public Void visitUnaryRule(final UnaryRule n) {
        switch (n.op.type) {
            case S_TD: {
                logMessage("FINE", "Negate next clause");

                final int negateSave = pushNewLocal(VarType.BOOL);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "getNegateFlag", "()Z", false);
                mv.visitVarInsn(ISTORE, negateSave);
                // Negate the current state: ~~'a' actually means 'a'
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitVarInsn(ILOAD, negateSave);
                ASMUtils.testIfElse(mv, IFNE, () -> mv.visitInsn(ICONST_1), () -> mv.visitInsn(ICONST_0));
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "setNegateFlag", "(Z)V", false);
                visit(n.rule);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitVarInsn(ILOAD, negateSave);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "setNegateFlag", "(Z)V", false);
                popLocal();
                return null;
            }
            case S_QM: {
                logMessage("FINE", "[0, 1] next clause");

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
                logMessage("FINE", "[1, n] next clause");

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
                logMessage("FINE", "[0, n] next clause");

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
            case S_LS: {
                logMessage("FINE", "Destructing Array or Collection:");

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "destructArray", "()Lcom/ymcmp/rset/rt/EvalState;", false);

                mv.visitInsn(DUP);
                ASMUtils.testIfElse(mv, IFNULL, () -> {
                    // save this.evalState,
                    final int save = pushNewLocal(VarType.EVAL_STATE);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                    mv.visitVarInsn(ASTORE, save);

                    // update this.state to the newly created state (TOP OF STACK),
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitInsn(SWAP);
                    mv.visitFieldInsn(PUTFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");

                    // test against the inner rules,
                    visit(n.rule);

                    // restore this.state
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ALOAD, save);
                    mv.visitFieldInsn(PUTFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                    popLocal();
                }, () -> {
                    // evalState is null, result is set to false because item was not destructable
                    mv.visitInsn(POP);
                    mv.visitInsn(ICONST_0);
                    mv.visitVarInsn(ISTORE, RESULT);
                });
                return null;
            }
            default:
                throw new RuntimeException("Unknown unary operator " + n.op);
        }
    }

    private void ldcRangeConstant(ValueNode node) {
        switch (node.token.type) {
            case L_CHARS: {
                final String str = node.toObject().toString();
                if (str.length() != 1) {
                    throw new RuntimeException("Invalid char range on " + str + ", length > 1");
                }
                mv.visitLdcInsn(str.charAt(0));
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            }
            case L_INT:
                mv.visitLdcInsn(node.toObject());
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case L_REAL:
                mv.visitLdcInsn(node.toObject());
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            case L_IDENT:
                mv.visitLdcInsn(node.toObject());
                break;
            default:
                throw new RuntimeException("Illegal load for " + node.token);
        }
    }

    public Void visitBinaryRule(final BinaryRule n) {
        switch (n.op.type) {
            case S_MN: {
                final ValueNode node1 = (ValueNode) n.rule1;
                final ValueNode node2 = (ValueNode) n.rule2;
                logMessage("FINE", "Test range of [" + node1.getText() + ", " + node2.getText() + "]");

                final int plst = findNearestLocal(VarType.LIST);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");

                try {
                    ldcRangeConstant(node1);
                    ldcRangeConstant(node2);
                } catch (IndexOutOfBoundsException ex) {
                    throw new RuntimeException("Broken char range on empty char sequence: " + node1.getText() + ", " + node2.getText());
                }

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
                logMessage("FINE", "Sequential clauses (" + ruleCount + " total):");

                final Label exit = new Label();
                final int out = findNearestLocal(VarType.LIST);
                final int lst = pushNewLocal(VarType.LIST);
                newObjectNoArgs(lst, "java/util/ArrayList");
                for (int i = 0; i < ruleCount; ++i) {
                    logMessage("FINER", "Sequential clause " + (i + 1) + " out of " + ruleCount + ":");

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
                logMessage("FINE", "Switch clauses (" + ruleCount + " total):");

                final Label exit = new Label();
                final Label epilogue = new Label();
                final int list = findNearestLocal(VarType.LIST);
                final int rwnd = pushNewLocal(VarType.NUM);

                final int negateState = pushNewLocal(VarType.BOOL);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "getNegateFlag", "()Z", false);
                mv.visitVarInsn(ISTORE, negateState);

                /*
                ~(a | b) true when input is neither a nor b
                ~a | ~b  true when input is either not a or not b

                Explaination for generated code:
                GENERALIZED_SWITCH_CLAUSE (boolean negateState, rule... init, rule last):
                $FOREACH rule in init
                    saveRoutine
                    result = test rule
                    if negateState {
                        if !result goto epilogue
                    } else {
                        if result goto exit
                    }
                    unsaveRoutine
                $ENDFOR

                    saveRoutine
                    result = test ~last
                    if result goto exit
                epilogue:
                    unsaveRoutine
                    result = false
                exit:
                */

                for (int i = 0; i < ruleCount - 1; ++i) {
                    logMessage("FINER", "Switch clause " + (i + 1) + " out of " + ruleCount + ":");

                    saveRoutine(list, rwnd);
                    visit(n.rules.get(i));

                    mv.visitVarInsn(ILOAD, negateState);
                    ASMUtils.testIfElse(mv, IFEQ, () -> {
                        mv.visitVarInsn(ILOAD, RESULT);
                        mv.visitJumpInsn(IFEQ, epilogue);
                    }, () -> {
                        mv.visitVarInsn(ILOAD, RESULT);
                        mv.visitJumpInsn(IFNE, exit);
                    });
                    unsaveRoutine(list, rwnd);
                };

                logMessage("FINER", "Switch clause " + ruleCount + " out of " + ruleCount + ":");

                saveRoutine(list, rwnd);
                visit(n.rules.get(ruleCount - 1));
                mv.visitVarInsn(ILOAD, RESULT);
                mv.visitJumpInsn(IFNE, exit);

                mv.visitLabel(epilogue);
                unsaveRoutine(list, rwnd);
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, RESULT);

                mv.visitLabel(exit);
                popLocal();
                popLocal();

                return null;
            }
            case GROUP: {
                final int ruleCount = n.rules.size();
                logMessage("FINE", "Grouping clauses (" + ruleCount + " total):");

                final Label br0 = new Label();
                final Label br1 = new Label();
                final int plst = findNearestLocal(VarType.LIST);
                final int list = pushNewLocal(VarType.LIST);
                final int rwnd = pushNewLocal(VarType.NUM);
                newObjectNoArgs(list, "java/util/ArrayList");
                saveStack(plst, rwnd);
                for (int i = 0; i < ruleCount; ++i) {
                    logMessage("FINER", "Group clause " + (i + 1) + " out of " + ruleCount + ":");

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
        logMessage("FINE", "Capturing next clause as " + dest);

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

        logMessage("FINE", "Entering rule " + name);

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

        logMessage("FINE", "Exiting rule " + name);

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