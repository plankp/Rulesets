/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

import java.util.Stack;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import com.ymcmp.lexparse.tree.Visitor;
import com.ymcmp.lexparse.tree.ParseTree;

import static org.objectweb.asm.Opcodes.*;

public abstract class BaseRuleVisitor extends Visitor<Void> implements ASMUtils {

    public enum VarType {
        HIDDEN, MAP, LIST, NUM, BOOL, EVAL_STATE;
    }

    private final Stack<VarType> locals = new Stack<>();

    protected final String className;
    protected final ClassWriter cw;

    public final boolean genDebugInfo;

    public MethodVisitor mv;

    public BaseRuleVisitor(ClassWriter cw, String className, boolean genDebugInfo) {
        this.cw = cw;
        this.className = className;
        this.genDebugInfo = genDebugInfo;
    }

    @Override
    public MethodVisitor getMethodVisitor() {
        return this.mv;
    }

    public int pushNewLocal(VarType t) {
        locals.push(t);
        return locals.size() - 1;
    }

    public void popLocal() {
        locals.pop();
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

    protected void saveStack(int listSlot, int rewindSlot) {
        mv.visitVarInsn(ALOAD, listSlot);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
        mv.visitVarInsn(ISTORE, rewindSlot);
    }

    protected void saveRoutine(int listSlot, int rewindSlot) {
        logMessage("FINER", "Save parse stack");

        saveStack(listSlot, rewindSlot);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "save", "()V", false);
    }

    protected void unsaveStack(int listSlot, int rewindSlot) {
        mv.visitVarInsn(ALOAD, listSlot);
        mv.visitVarInsn(ILOAD, rewindSlot);
        mv.visitVarInsn(ALOAD, listSlot);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "subList", "(II)Ljava/util/List;", true);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "clear", "()V", true);
    }

    protected void unsaveRoutine(int listSlot, int rewindSlot) {
        logMessage("FINER", "Restore parse stack");

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "unsave", "()V", false);
        unsaveStack(listSlot, rewindSlot);
    }

    protected void invokeEvalStateNoObject(int resultSlot, String methodName) {
        final int plst = findNearestLocal(VarType.LIST);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
        mv.visitVarInsn(ALOAD, plst);
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", methodName, "(Ljava/util/Collection;)Z", false);
        mv.visitVarInsn(ISTORE, resultSlot);
    }

    protected void addToParseStack(final int list, final int plst) {
        mv.visitVarInsn(ALOAD, plst);
        mv.visitVarInsn(ALOAD, list);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
        mv.visitInsn(POP);
    }

    protected void decSizeAndDup(final int plst) {
        mv.visitVarInsn(ALOAD, plst);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ISUB);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
    }
}