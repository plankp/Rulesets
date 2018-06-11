package com.ymcmp.rset.tree;

import com.ymcmp.rset.Type;

import com.ymcmp.engine.Token;
import com.ymcmp.engine.tree.ParseTree;

public final class ValueNode extends ParseTree {

    public final Token<Type> token;

    public ValueNode(Token<Type> token) {
        this.token = token;
    }

    @Override
    public ParseTree getChild(int node) {
        throw new IndexOutOfBoundsException(node);
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
                return token.text.length() == 1
                    ? token.text.charAt(0)
                    : token.text;
            case L_NUMBER:
                return Integer.parseInt(token.text);
            default:
                throw new RuntimeException("Unknown value type " + token.type);
        }
    }
}