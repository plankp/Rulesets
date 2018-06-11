package com.ymcmp.engine;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

import com.ymcmp.engine.tree.ParseTree;

public interface Parser<T extends Enum<T>, R extends ParseTree> {

    public Lexer<T> getLexer();

    public Token<T> getToken();

    public void ungetToken(Token<T> tok);

    public R parse();

    public default Token<T> consumeToken(T type, String messageWhenUnmatched) {
        final Token<T> token = getToken();
        if (token == null || token.type != type) {
            throw new RuntimeException(messageWhenUnmatched);
        }
        return token;
    }

    public default <R> R consumeRule(Supplier<? extends R> rule, String messageWhenUnmatched) {
        final R t = rule.get();
        if (t == null) {
            throw new RuntimeException(messageWhenUnmatched);
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
                final Token t = getToken();
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
}