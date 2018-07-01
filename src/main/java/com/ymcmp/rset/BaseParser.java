/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.util.List;

import java.util.function.Supplier;

import com.ymcmp.rset.tree.*;

import com.ymcmp.lexparse.Token;
import com.ymcmp.lexparse.Parser;
import com.ymcmp.lexparse.IllegalParseException;

import com.ymcmp.lexparse.tree.ParseTree;

public abstract class BaseParser<T extends ParseTree> implements Parser<Type, T> {

    protected ParseTree termNormalizingSequence(KaryRule.Type combineType, Type sep, Supplier<? extends ParseTree> rule) {
        final List<ParseTree> elms = consumeRules(sep, rule);
        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);

        if (sep != null) {
            // Only with non-null separators can null terms be a thing
            final int k = elms.size() - 1;
            if (elms.get(k) == null) elms.remove(k);
        }
        return new KaryRule(combineType, elms);
    }

    protected ParseTree termPreservingSequence(KaryRule.Type combineType, String exprName, Type sep, Supplier<? extends ParseTree> rule) {
        final List<ParseTree> elms = consumeRules(sep, rule);
        if (elms == null) return null;
        if (elms.size() == 1) return elms.get(0);

        if (elms.get(elms.size() - 1) == null) {
            throw new IllegalParseException("Incomplete " + exprName + " expression, missing rhs");
        }
        return new KaryRule(combineType, elms);
    }

    public ValueNode parseValue() {
        final Token<Type> t = getToken();
        if (t != null) {
            switch (t.type) {
                case S_MN: {
                    final Token<Type> inner = getToken();
                    if (inner != null && inner.type.isNumeric()) {
                        return new ValueNode(new Token<>(inner.type, '-' + inner.text));
                    }
                    throw new IllegalParseException("Expected numerical value after negative sign", inner);
                }
                case S_MD: {
                    final Token<Type> inner = getToken();
                    if (inner != null && inner.type.isValue()) {
                        return new ValueNode(new Token<>(Type.L_CHARS, '%' + inner.text));
                    }
                    throw new IllegalParseException("Expected constant after '%'", inner);
                }
                default:
                    if (t.type.isValue()) return new ValueNode(t);
                    ungetToken(t);
            }
        }
        return null;
    }
}