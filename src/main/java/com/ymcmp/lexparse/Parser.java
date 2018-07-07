/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.lexparse;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

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
            throw new IllegalParseException(messageWhenUnmatched, token);
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

    public default <R> List<R> consumeRules(T delim, Supplier<? extends R> rule) {
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
            if (el == null && delim == null) break;
            elms.add(el);
        }
        return elms;
    }

    public default <R> R consumeRules(TriFunction<? super Token<T>, ? super R, ? super R, ? extends R> fn,
                                      Collection<T> delim, Supplier<? extends R> rule) {
        R head = rule.get();
        while (head != null) {
            Token<T> t = getToken();
            if (t == null || !delim.contains(t.type)) {
                ungetToken(t);
                break;
            }
            head = fn.apply(t, head, rule.get());
        }
        return head;
    }
}