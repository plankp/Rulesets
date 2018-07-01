/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

import java.util.Map;
import java.util.HashMap;

import com.ymcmp.rset.Type;

import com.ymcmp.lexparse.Token;
import com.ymcmp.lexparse.tree.ParseTree;

public final class ValueNode extends ParseTree {

    public final Token<Type> token;

    public ValueNode(Token<Type> token) {
        this.token = token;
    }

    @Override
    public ParseTree getChild(int node) {
        throw new IndexOutOfBoundsException("ValueNode does not have children: " + node);
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String getText() {
        return token.text;
    }

    public Object toObject() {
        switch (token.type) {
            case L_IDENT:
                return escape(token.text);
            case L_CHARS:
                return escape(token.text.substring(1));
            case L_INT:
                return Integer.parseInt(token.text);
            case L_REAL:
                return Double.parseDouble(token.text);
            case L_NULL:
                return null;
            default:
                throw new RuntimeException("Unknown value type " + token.type);
        }
    }

    public String toJavaLiteral() {
        switch (token.type) {
            case L_IDENT:
                return '"' + token.text + '"';
            case L_CHARS:
                return '"' + token.text.substring(1) + "\".toCharArray()";
            case L_INT:
            case L_REAL:
                return token.text;
            case L_NULL:
                return "null";
            default:
                throw new RuntimeException("Unknown value type " + token.type);
        }
    }

    private static final Map<Character, Character> ESC_MAPPING = new HashMap<Character, Character>() {{
        put('\\', '\\'); put('\'', '\''); put('"', '"');
        put('b', '\b'); put('t', '\t'); put('n', '\n');
        put('f', '\f'); put('r', '\r');
        put('a', '\u0007'); put('v', '\u000b');
    }};

    private static String escape(String text) {
        final char[] arr = text.toCharArray();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; ++i) {
            final char k = arr[i];
            if (k == '\\') {    // Escape sequences
                final char e = arr[++i];
                final Character mapping = ESC_MAPPING.get(e);
                if (mapping == null) {
                    throw new RuntimeException("Bad string escape \\" + e);
                }
                sb.append(mapping);
            } else {
                sb.append(k);
            }
        }
        return sb.toString();
    }
}