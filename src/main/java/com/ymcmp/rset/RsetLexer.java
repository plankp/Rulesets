/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import java.io.Reader;
import java.io.Closeable;
import java.io.IOException;

import com.ymcmp.lexparse.Lexer;
import com.ymcmp.lexparse.Token;

public class RsetLexer implements Lexer<Type>, Closeable {

    private static final Set<Character> STR_ESC = new HashSet<Character>() {{
        add('\\'); add('\''); add('"');
        add('a'); add('b'); add('t'); add('n');
        add('v'); add('f'); add('r');
    }};

    private static final Map<Character, Type> MONO_OP = new HashMap<Character, Type>() {{
        put(',', Type.S_CM); put('=', Type.S_EQ); put('-', Type.S_MN);
        put('+', Type.S_AD); put('?', Type.S_QM); put(':', Type.S_CO);
        put('|', Type.S_OR); put('~', Type.S_TD); put('*', Type.S_ST);
        put('/', Type.S_DV); put('%', Type.S_MD); put('!', Type.S_EX);
        put('&', Type.S_AM); put('(', Type.S_LP); put(')', Type.S_RP);
        put('[', Type.S_LS); put(']', Type.S_RS); put('{', Type.S_LB);
        put('}', Type.S_RB);
    }};
 
    private Reader reader;
    private int buf = -1;

    public RsetLexer(final Reader reader) {
        this.reader = reader;
    }

    @Override
    public Token<Type> nextToken() {
        int rc;
        while ((rc = read()) != -1) {
            final char c = (char) rc;
            switch (c) {
                case '#':
                    readWhile(k -> !Lexer.isEOL(k));
                case ' ':
                case '\n':
                case '\r':
                case '\t':
                    continue;
                case '\'':
                case '"':
                    return lexRawString(c);
                default: {
                    final Type type = MONO_OP.get(c);
                    if (type != null) {
                        return new Token<>(type, Character.toString(c));
                    }

                    unread(c);
                    final String t = readWhile(Character::isDigit);
                    final int k = read();
                    if (k == '.') {
                        final String frac = readWhile(Character::isDigit);
                        if (!frac.isEmpty()) {
                            return new Token<>(Type.L_REAL, t + '.' + frac);
                        }
                    } else {
                        unread(k);
                    }
                    if (!t.isEmpty()) {
                        return new Token<>(Type.L_INT, t);
                    }

                    final String i = readWhile(RsetLexer::isIdent);
                    if (!i.isEmpty()) {
                        return new Token<>(Type.L_IDENT, i);
                    }

                    throw new RuntimeException("Unknown char " + c);
                }
            }
        }
        return null;
    }

    private Token<Type> lexRawString(char quoteMark) {
        final StringBuilder sb = new StringBuilder();
        // unterminated strings are considered complete
        for (int k = read(); k != -1 && k != quoteMark; k = read()) {
            if (k == '\\') {    // Escape sequences
                final int e = read();
                if (e == -1) {
                    break;
                }

                final char c = (char) e;
                if (!STR_ESC.contains(c)) {
                    sb.append('\\');
                }
                sb.append('\\').append(c);
            } else {
                if (k == '\'' || k == '\"') {
                    // escape it, avoids problems
                    sb.append('\\');
                }
                sb.append((char) k);
            }
        }
        return new Token<>(Type.L_IDENT, sb.toString());
    }

    @Override
    public int read() {
        try {
            if (buf == -1) return reader.read();
            final int k = buf;
            buf = -1;
            return k;
        } catch (IOException ex) {
            return -1;
        }
    }

    @Override
    public void unread(final int k) {
        if (buf != -1) throw new RuntimeException("Not buffering > 1 chars");
        buf = k;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public static boolean isIdent(final int id) {
        return id >= 'a' && id <= 'z'
            || id >= 'A' && id <= 'Z'
            || id == '$' || id == '_'
            || Character.isDigit(id);
    }
}