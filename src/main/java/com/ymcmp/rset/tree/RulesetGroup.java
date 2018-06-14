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

import com.ymcmp.rset.tree.BytecodeRuleVisitor.VarType;

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
        return toBytecode(className, null);
    }

    public byte[] toBytecode(final String className, final String sourceFile) {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        final Stack<String> fragmentStack = new Stack<>();

        final BytecodeActionVisitor aw = new BytecodeActionVisitor(cw, className);
        final BytecodeRuleVisitor rw = new BytecodeRuleVisitor(cw, className, rsets.stream()
                .collect(Collectors.toMap(e -> e.name.getText(), e -> {
                    switch (e.type) {
                        case RULE:
                            return vis -> {
                                final String testName = "test" + e.name.getText();
                                final int map = vis.findNearestLocal(VarType.MAP);
                                vis.mv.visitVarInsn(ALOAD, 0);
                                vis.mv.visitVarInsn(ALOAD, map);
                                vis.mv.visitMethodInsn(INVOKEVIRTUAL, className, testName, "(Ljava/util/Map;)Z", false);
                                vis.mv.visitVarInsn(ISTORE, 2);
                            };
                        case SUBRULE:
                            return vis -> {
                                final String name = e.name.getText();
                                final int map = vis.findNearestLocal(VarType.MAP);
                                final int localEnv = vis.pushNewLocal(VarType.MAP);
                                final Label exit = new Label();
                                vis.mv.visitTypeInsn(NEW, "java/util/HashMap");
                                vis.mv.visitInsn(DUP);
                                vis.mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
                                vis.mv.visitVarInsn(ASTORE, localEnv);
                                vis.mv.visitVarInsn(ALOAD, 0);
                                vis.mv.visitVarInsn(ALOAD, localEnv);
                                vis.mv.visitMethodInsn(INVOKEVIRTUAL, className, "test" + name, "(Ljava/util/Map;)Z", false);
                                vis.mv.visitInsn(DUP);
                                vis.mv.visitJumpInsn(IFEQ, exit);
                                vis.mv.visitVarInsn(ALOAD, map);
                                vis.mv.visitLdcInsn(name);
                                vis.mv.visitVarInsn(ALOAD, 0);
                                vis.mv.visitVarInsn(ALOAD, localEnv);
                                vis.mv.visitMethodInsn(INVOKEVIRTUAL, className, "act" + name, "(Ljava/util/Map;)Ljava/lang/Object;", false);
                                vis.mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                                vis.mv.visitInsn(POP);
                                vis.mv.visitLabel(exit);
                                vis.popLocal();
                                vis.mv.visitVarInsn(ISTORE, 2);
                            };
                        case FRAGMENT:
                            return vis -> {
                                final String name = e.name.getText();
                                if (fragmentStack.contains(name)) {
                                    throw new RuntimeException("Recursive fragment definition via " + fragmentStack + " -> " + name);
                                }
                                fragmentStack.push(name);
                                vis.visit(e.rule);
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

        final MethodVisitor ctor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode();
        // super();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        // state = new EvalState(new Object[0]);
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitTypeInsn(NEW, "com/ymcmp/rset/rt/EvalState");
        ctor.visitInsn(DUP);
        ctor.visitInsn(ICONST_0);
        ctor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        ctor.visitMethodInsn(INVOKESPECIAL, "com/ymcmp/rset/rt/EvalState", "<init>", "([Ljava/lang/Object;)V", false);
        ctor.visitFieldInsn(PUTFIELD, className, "state", "Lcom/ymcmp/rset/rt/EvalState;");
        // ext = new Extensions();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitTypeInsn(NEW, "com/ymcmp/rset/lib/Extensions");
        ctor.visitInsn(DUP);
        ctor.visitMethodInsn(INVOKESPECIAL, "com/ymcmp/rset/lib/Extensions", "<init>", "()V", false);
        ctor.visitFieldInsn(PUTFIELD, className, "ext", "Lcom/ymcmp/rset/lib/Extensions;");
        // rules = new HashMap<>();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitTypeInsn(NEW, "java/util/HashMap");
        ctor.visitInsn(DUP);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
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
            // if (%test(env@2)) %if-block else %else-block
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, testName, "(Ljava/util/Map;)Z", false);
            final Label label = new Label();
            mv.visitJumpInsn(IFEQ, label);
            // %if-block -> return (%act(env@2))
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, actnName, "(Ljava/util/Map;)Ljava/lang/Object;", false);
            mv.visitInsn(ARETURN);
            // %else-block
            mv.visitLabel(label);
            mv.visitInsn(ACONST_NULL);
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
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Lcom/ymcmp/rset/rt/Rule;", true);
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