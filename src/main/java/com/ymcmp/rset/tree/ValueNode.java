/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset.tree;

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

    private static String escape(String text) {
        final char[] arr = text.toCharArray();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; ++i) {
            final char k = arr[i];
            switch (k) {
                case '\\': {    // Escape sequences
                    final char e = arr[++i];
                    switch (e) {
                        case '\\':
                        case '\'':
                        case '"': sb.append(e); break;
                        case 'a': sb.append('\u0007'); break;
                        case 'b': sb.append('\b'); break;
                        case 't': sb.append('\t'); break;
                        case 'n': sb.append('\n'); break;
                        case 'v': sb.append('\u000b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'r': sb.append('\r'); break;
                        default: throw new RuntimeException("Bad string escape \\" + e);
                    }
                    break;
                } 
                default:
                    sb.append(k);
            }
        }
        return sb.toString();
    }
}