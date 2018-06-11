package com.ymcmp.lexparse;

import java.util.List;
import java.util.ArrayList;

import java.util.function.Supplier;

import com.ymcmp.function.TriFunction;

import com.ymcmp.lexparse.tree.ParseTree;

public interface Parser<T extends Enum<T>, R extends ParseTree> {

    public Lexer<T> getLexer();

    public Token<T> getToken();

    public void ungetToken(Token<T> tok);

    public R parse();

    public default Token<T> peekToken() {
        final Token<T> t = getToken();
        ungetToken(t);
        return t;
    }

    public default Token<T> consumeToken(T type, String messageWhenUnmatched) {
        final Token<T> token = getToken();
        if (token == null || token.type != type) {
            throw new IllegalParseException(messageWhenUnmatched);
        }
        return token;
    }

    public default <R> R consumeRule(Supplier<? extends R> rule, String messageWhenUnmatched) {
        final R t = rule.get();
        if (t == null) {
            throw new IllegalParseException(messageWhenUnmatched);
        }
        return t;
    }

    public default <R> List<R> consumeRules(Supplier<? extends R> rule, T delim) {
        final R head = rule.get();
        if (head == null) return null;

        final List<R> elms = new ArrayList<>();
        elms.add(head);

        while (true) {
            if (delim != null) {
                final Token<T> t = getToken();
                if (t == null || t.type != delim) {
                    ungetToken(t);
                    break;
                }
            }
            final R el = rule.get();
            if (el == null) {
                if (delim != null) elms.add(null);
                break;
            }
            elms.add(el);
        }
        return elms;
    }

    public default <R> R consumeRules(TriFunction<? super Token<T>, ? super R, ? super R, ? extends R> fn,
                                      Supplier<? extends R> rule, List<T> delim) {
        R head = rule.get();
        if (head == null) return null;

        while (true) {
            Token<T> t = null;
            if (!delim.isEmpty()) {
                t = getToken();
                if (t == null || !delim.contains(t.type)) {
                    ungetToken(t);
                    break;
                }
            }
            final R tail = rule.get();
            if (tail == null) {
                if (!delim.isEmpty()) {
                    head = fn.apply(t, head, tail);
                }
                break;
            }
            head = fn.apply(t, head, tail);
        }
        return head;
    }
}