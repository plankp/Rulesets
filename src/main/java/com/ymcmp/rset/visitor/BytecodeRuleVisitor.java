/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.visitor;

import java.util.Map;
import java.util.List;
import java.util.function.Consumer;

import org.objectweb.asm.Label;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import com.ymcmp.rset.Type;
import com.ymcmp.rset.Scope;
import com.ymcmp.rset.Scope.VarType;

import com.ymcmp.rset.tree.*;

import com.ymcmp.lexparse.tree.ParseTree;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeRuleVisitor extends BaseRuleVisitor {

    private final Map<String, Consumer<BytecodeRuleVisitor>> refs;

    public int RESULT;

    public BytecodeRuleVisitor(ClassWriter cw, String className, boolean genDebugInfo, Map<String, Consumer<BytecodeRuleVisitor>> refs) {
        super(cw, className, genDebugInfo);
        this.refs = refs;
    }

    private void loadEvalState() {
        selfGetField(className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
    }

    private void callEvalStateTest(final int localParseStack, final String name, final String params) {
        mv.visitVarInsn(ALOAD, localParseStack);
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", name, params, false);
        mv.visitVarInsn(ISTORE, RESULT);
    }

    public void visitRefRule(final RefRule n) {
        final String name = n.node.getText();
        final Consumer<BytecodeRuleVisitor> cons = refs.get(name);
        if (cons == null) throw new RuntimeException("Attempt to reference undeclared rule " + name);
        cons.accept(this);
    }

    public void visitValueNode(final ValueNode n) {
        switch (n.token.type) {
            case S_ST:
                logMessage("FINE", "Test wildcard slot");
                invokeEvalStateNoObject(RESULT, "testSlotOccupied");
                break;
            case S_SM:
                logMessage("FINE", "Test end of data");
                invokeEvalStateNoObject(RESULT, "testEnd");
                break;
            default: {
                logMessage("FINE", "Test for " + n.getText());

                final int plst = scope.findNearestLocal(VarType.LIST);
                loadEvalState();

                pushAsObject(n);

                if (n.token.type == Type.L_CHARS) {
                    // generate a loop that tests equality on every index
                    // %abc is short-hand for %a %b %c which becomes
                    // testEquality((Character) 'a') testEquality((Character) 'b') testEquality((Character) 'c')

                    final int arrChars = scope.pushNewLocal(VarType.LIST);
                    final int lst = scope.pushNewLocal(VarType.LIST);
                    mv.visitVarInsn(ASTORE, arrChars);
                    newObjectNoArgs(lst, "java/util/ArrayList");
                    storeBool(RESULT, false);

                    // Strings are immutable, compute length at compile time
                    fixedIntCounter(scope.pushNewLocal(VarType.NUM), 0, n.toObject().toString().length(), (i, exit, loop) -> {
                        loadEvalState();
                        mv.visitVarInsn(ALOAD, arrChars);
                        mv.visitVarInsn(ILOAD, i);
                        mv.visitInsn(CALOAD);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                        callEvalStateTest(lst, "testEquality", "(Ljava/lang/Object;Ljava/util/Collection;)Z");
                        jumpIfBoolFalse(RESULT, exit);
                    });
                    mv.visitInsn(POP);
                    addToParseStack(lst, plst);
                    scope.popLocal();
                    scope.popLocal();
                    scope.popLocal();
                } else {
                    // Other data types just call testEquality, nothing special is needed

                    callEvalStateTest(plst, "testEquality", "(Ljava/lang/Object;Ljava/util/Collection;)Z");
                }
            }
        }
    }

    private void testInheritanceRoutine(final ValueNode r, final boolean from) {
        try {
            final String cl = r.toObject().toString();
            logMessage("FINE", from
                    ? ("Test if class of slot inherits from class '" + cl + "'")
                    : ("Test if class '" + cl + "' inherits from class of slot"));
            // load the class at runtime instead of compile time
            loadEvalState();
            mv.visitLdcInsn(cl);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
            mv.visitInsn(from ? ICONST_1 : ICONST_0);
            callEvalStateTest(scope.findNearestLocal(VarType.LIST), "testInheritance", "(Ljava/lang/Class;ZLjava/util/Collection;)Z");
        } catch (NullPointerException ex) {
            throw new RuntimeException("() does not name a class");
        }
    }

    public void visitUnaryRule(final UnaryRule n) {
        switch (n.op.type) {
            case S_TD: {
                logMessage("FINE", "Negate next clause");

                final int negateSave = scope.pushNewLocal(VarType.BOOL);
                loadEvalState();
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "getNegateFlag", "()Z", false);
                mv.visitVarInsn(ISTORE, negateSave);
                // Negate the current state: ~~'a' actually means 'a'
                loadEvalState();
                mv.visitVarInsn(ILOAD, negateSave);
                testIfElse(IFNE, () -> mv.visitInsn(ICONST_1), () -> mv.visitInsn(ICONST_0));
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "setNegateFlag", "(Z)V", false);
                visit(n.rule);
                loadEvalState();
                mv.visitVarInsn(ILOAD, negateSave);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "setNegateFlag", "(Z)V", false);
                scope.popLocal();
                break;
            }
            case S_QM: {
                logMessage("FINE", "[0, 1] next clause");

                final int list = scope.findNearestLocal(VarType.LIST);
                final int rwnd = scope.pushNewLocal(VarType.NUM);
                saveRoutine(list, rwnd);
                visit(n.rule);
                ifBoolElse(RESULT, this::updateSaveRoutine, () -> unsaveRoutine(list, rwnd));
                storeBool(RESULT, true);
                scope.popLocal();
                break;
            }
            case S_AD:
                testNCases(n.rule, RESULT, false);
                break;
            case S_ST:
                testNCases(n.rule, RESULT, true);
                break;
            case S_EX:
                try {
                    final String selector = ((ValueNode) n.rule).toObject().toString();
                    logMessage("FINE", "Test if object responds to selector '" + selector + "'");

                    loadEvalState();
                    mv.visitLdcInsn(selector);
                    callEvalStateTest(scope.findNearestLocal(VarType.LIST), "hasFieldOrMethod", "(Ljava/lang/String;Ljava/util/Collection;)Z");
                    break;
                } catch (NullPointerException ex) {
                    throw new RuntimeException("Selector cannot be ()");
                }
            case S_LA:
                testInheritanceRoutine((ValueNode) n.rule, true);
                break;
            case S_RA:
                testInheritanceRoutine((ValueNode) n.rule, false);
                break;
            case S_LS: {
                logMessage("FINE", "Destructing Array or Collection:");

                loadEvalState();
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/ymcmp/rset/rt/EvalState", "destructArray", "()Lcom/ymcmp/rset/rt/EvalState;", false);

                mv.visitInsn(DUP);
                testIfElse(IFNULL, () -> {
                    // save this.evalState,
                    final int save = scope.pushNewLocal(VarType.EVAL_STATE);
                    loadEvalState();
                    mv.visitVarInsn(ASTORE, save);

                    // update this.state to the newly created state (TOP OF STACK),
                    selfPutField(className, "state", "Lcom/ymcmp/rset/rt/EvalState;");

                    // test against the inner rules,
                    visit(n.rule);

                    // restore this.state
                    mv.visitVarInsn(ALOAD, save);
                    selfPutField(className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
                    scope.popLocal();
                }, () -> {
                    // evalState is null, result is set to false because item was not destructable
                    mv.visitInsn(POP);
                    storeBool(RESULT, false);
                });
                break;
            }
            default:
                throw new RuntimeException("Unknown unary operator " + n.op);
        }
    }

    private void ldcRangeConstant(ValueNode node) {
        pushAsObject(node);
        if (node.token.type == Type.L_CHARS) {
            final String str = node.toObject().toString();
            if (str.length() != 1) {
                throw new RuntimeException("Invalid char range on " + str + ", length > 1");
            }
            mv.visitInsn(ICONST_0);
            mv.visitInsn(CALOAD);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
        }
    }

    public void visitBinaryRule(final BinaryRule n) {
        switch (n.op.type) {
            case S_MN: {
                final ValueNode node1 = (ValueNode) n.rule1;
                final ValueNode node2 = (ValueNode) n.rule2;
                logMessage("FINE", "Test range of [" + node1.getText() + ", " + node2.getText() + "]");

                loadEvalState();
                ldcRangeConstant(node1);
                ldcRangeConstant(node2);
                callEvalStateTest(scope.findNearestLocal(VarType.LIST),
                        "testRange", "(Ljava/lang/Comparable;Ljava/lang/Comparable;Ljava/util/Collection;)Z");
                break;
            }
            default:
                throw new RuntimeException("Unknown binary operator " + n.op);
        }
    }

    public void visitRuleSeq(final List<ParseTree> rules) {
        final int ruleCount = rules.size();
        logMessage("FINE", "Sequential clauses (" + ruleCount + " total):");

        final Label exit = new Label();
        final int out = scope.findNearestLocal(VarType.LIST);
        final int lst = scope.pushNewLocal(VarType.LIST);
        newObjectNoArgs(lst, "java/util/ArrayList");
        for (int i = 0; i < ruleCount; ++i) {
            logMessage("FINER", "Sequential clause " + (i + 1) + " out of " + ruleCount + ":");

            visit(rules.get(i));
            jumpIfBoolFalse(RESULT, exit);
        }
        storeBool(RESULT, true);
        mv.visitLabel(exit);

        addToParseStack(lst, out);
        scope.popLocal();
    }

    public void visitRuleSwitch(final List<ParseTree> rules) {
        final int ruleCount = rules.size();
        logMessage("FINE", "Switch clauses (" + ruleCount + " total):");

        final Label exit = new Label();
        final Label epilogue = new Label();
        final Label end = new Label();
        final int list = scope.findNearestLocal(VarType.LIST);
        final int rwnd = scope.pushNewLocal(VarType.NUM);

        final int negateState = scope.pushNewLocal(VarType.BOOL);
        loadEvalState();
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
            goto end
        exit:
            updateSaveRoutine
        end:
        */

        for (int i = 0; i < ruleCount - 1; ++i) {
            logMessage("FINER", "Switch clause " + (i + 1) + " out of " + ruleCount + ":");

            saveRoutine(list, rwnd);
            visit(rules.get(i));

            ifBoolElse(negateState,
                    () -> jumpIfBoolFalse(RESULT, epilogue),
                    () -> jumpIfBoolTrue(RESULT, exit));
            unsaveRoutine(list, rwnd);
        };

        logMessage("FINER", "Switch clause " + ruleCount + " out of " + ruleCount + ":");

        saveRoutine(list, rwnd);
        visit(rules.get(ruleCount - 1));

        ifBoolFalse(RESULT, exit, () -> {
            mv.visitLabel(epilogue);
            unsaveRoutine(list, rwnd);
            storeBool(RESULT, false);
            mv.visitJumpInsn(GOTO, end);
        });
        updateSaveRoutine();
        mv.visitLabel(end);

        scope.popLocal();
        scope.popLocal();
    }

    public void visitRuleGroup(final List<ParseTree> rules) {
        final int ruleCount = rules.size();
        logMessage("FINE", "Grouping clauses (" + ruleCount + " total):");

        final Label br0 = new Label();
        final Label br1 = new Label();
        final int plst = scope.findNearestLocal(VarType.LIST);
        final int list = scope.pushNewLocal(VarType.LIST);
        final int rwnd = scope.pushNewLocal(VarType.NUM);
        newObjectNoArgs(list, "java/util/ArrayList");
        saveStack(plst, rwnd);
        for (int i = 0; i < ruleCount; ++i) {
            logMessage("FINER", "Group clause " + (i + 1) + " out of " + ruleCount + ":");

            visit(rules.get(i));
            jumpIfBoolFalse(RESULT, br0);
            decSizeAndDup(plst);
            testIf(IF_ICMPLT, () -> {
                mv.visitVarInsn(ALOAD, list);
                mv.visitInsn(SWAP);
                mv.visitVarInsn(ALOAD, plst);
                mv.visitInsn(SWAP);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "remove", "(I)Ljava/lang/Object;", true);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            });
            mv.visitInsn(POP);
        }

        addToParseStack(list, plst);
        mv.visitJumpInsn(GOTO, br1);
        mv.visitLabel(br0);
        unsaveStack(plst, rwnd);
        mv.visitLabel(br1);
        scope.popLocal();
        scope.popLocal();
    }

    public void visitCaptureRule(final CaptureRule n) {
        final Label exit = new Label();
        final String dest = n.dest.getText();
        final int plst = scope.findNearestLocal(VarType.LIST);
        final int map = scope.findNearestLocal(VarType.MAP);
        logMessage("FINE", "Capturing next clause as " + dest);

        visit(n.rule);
        mv.visitVarInsn(ALOAD, map);    // Setting up stack for map.put
        mv.visitLdcInsn(dest);          // Setting up stack for map.put(dest

        ifBoolTrue(RESULT, () -> {
            mv.visitVarInsn(ALOAD, plst);
            decSizeAndDup(plst);
            testIf(IF_ICMPLT, () -> {
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
                mv.visitJumpInsn(GOTO, exit);
            });
            mv.visitInsn(POP2);
        });

        mv.visitFieldInsn(GETSTATIC, "java/util/Collections", "EMPTY_LIST", "Ljava/util/List;");
        mv.visitLabel(exit);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitInsn(POP);
    }

    public void visitRulesetNode(final RulesetNode n) {
        final String name = n.name.getText();
        final String testName = n.makeTestName().get();
        mv = cw.visitMethod(ACC_PUBLIC, testName, "(Ljava/util/Map;Ljava/util/List;)Z", "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;Ljava/util/List<Ljava/lang/Object;>;)Z", null);
        mv.visitCode();

        scope.pushNewLocal(VarType.HIDDEN);  // this
        final int env = scope.pushNewLocal(VarType.MAP);      // env
        final int lst = scope.pushNewLocal(VarType.LIST);     // lst
        final int map = scope.pushNewLocal(VarType.MAP);
        RESULT = scope.pushNewLocal(VarType.BOOL);

        // Initialize objects
        newObjectNoArgs(map, "java/util/HashMap");

        logMessage("FINE", "Entering rule " + name);

        // Fill in the actual clauses here!
        visit(n.rule);
        // if (env@1 != null) %additional
        mv.visitVarInsn(ALOAD, env);
        testIf(IFNULL, () -> {
            // %additional -> env@1.putAll(captures@3)
            mv.visitVarInsn(ALOAD, env);
            mv.visitVarInsn(ALOAD, map);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "putAll", "(Ljava/util/Map;)V", true);
        });

        logMessage("FINE", "Exiting rule " + name);

        // return result@2
        mv.visitVarInsn(ILOAD, RESULT);
        mv.visitInsn(IRETURN);

        scope.popLocal();
        scope.popLocal();
        scope.popLocal();
        scope.popLocal();
        scope.popLocal();

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}