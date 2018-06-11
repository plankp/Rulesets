package com.ymcmp.rset.tree;

import java.util.Map;
import java.util.HashMap;

import java.util.stream.Stream;

import java.util.function.Function;
import java.util.function.Supplier;

import com.ymcmp.rset.ExprAction;
import com.ymcmp.rset.lib.Stdlib;

import com.ymcmp.engine.tree.Visitor;
import com.ymcmp.engine.tree.ParseTree;

public class ActionVisitor extends Visitor<ExprAction> {

    public ExprAction visitMethodNotFound(final ParseTree tree) {
        throw new RuntimeException(tree.getClass().getSimpleName() + " cannot be converted to action");
    }

    public ExprAction visitValueNode(ValueNode n) {
        final Object obj = n.toObject();
        return (env) -> obj;
    }

    public ExprAction visitUnaryRule(UnaryRule n) {
        final ExprAction ref = visit(n.rule);
        switch (n.op.type) {
            case S_ST:
                return (env) -> env.get(ref.apply(env).toString());
            default:
                throw new RuntimeException("Unknown unary operator " + n.op);
        }
    }

    public ExprAction visitKaryRule(final KaryRule n) {
        final Supplier<Stream<ExprAction>> rules = () -> n.rules
                .stream()
                .map(this::visit);
        switch (n.type) {
            case SUBSCRIPT:
                return (env) -> rules.get().map(e -> e.apply(env))
                        .reduce(Stdlib::subscript)
                        .orElse(null);
            case JOIN:
                return (env) -> Stdlib.concat(rules.get().map(e -> e.apply(env))
                        .toArray(Object[]::new));
            case ARRAY:
                return (env) -> rules.get().map(e -> e.apply(env)).toArray();
            case CALL:
                return (env) -> {
                    final Object[] arr = rules.get().map(e -> e.apply(env)).toArray(Object[]::new);
                    final Object[] args = new Object[arr.length - 1];
                    System.arraycopy(arr, 1, args, 0, args.length);
                    return ((Function<Object[], ?>) arr[0]).apply(args);
                };
            case AND: {
                final ExprAction[] elms = rules.get().toArray(ExprAction[]::new);
                return (env) -> {
                    Object u = null;
                    for (int i = 0; i < elms.length; ++i) {
                        u = elms[i].apply(env);
                        if (!Stdlib.isTruthy(u)) return u;
                    }
                    return u;
                };
            }
            case OR: {
                final ExprAction[] elms = rules.get().toArray(ExprAction[]::new);
                return (env) -> {
                    Object u = null;
                    for (int i = 0; i < elms.length; ++i) {
                        u = elms[i].apply(env);
                        if (Stdlib.isTruthy(u)) return u;
                    }
                    return u;
                };
            }
            case ASSIGN: {
                // NOTE: This implementation does not mutate maps or arrays
                final ExprAction[] elms = rules.get().toArray(ExprAction[]::new);
                return (env) -> {
                    Object u = elms[0].apply(env);
                    for (int i = 1; i < elms.length; ++i) {
                        final Object k = elms[i].apply(env);
                        env.put(u.toString(), k);
                        u = k;
                    }
                    return u;
                };
            }
            default:
                throw new RuntimeException("Unknown rule block " + n.type);
        }
    }
}