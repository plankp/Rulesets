/**
 * Copyright (c) 2018 Paul Teng <plankp@outlook.com>.
 * Licensed under the BSD-3-Clause License - https://raw.githubusercontent.com/plankp/Rulesets/blob/master/LICENSE
 */

package com.ymcmp.rset;

import java.io.Reader;
import java.io.Closeable;
import java.io.IOException;

import java.nio.BufferOverflowException;

import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.AbstractMap.SimpleEntry;

import java.util.stream.Stream;
import java.util.stream.Collectors;

import com.ymcmp.lexparse.Lexer;
import com.ymcmp.lexparse.Token;

public class RsetLexer implements Lexer<Type>, Closeable {

    private static final Set<Character> STR_ESC =
            Stream.of('\\', '\'', '"', 'a', 'b', 't', 'n', 'v', 'f', 'r')
                    .collect(Collectors.toSet());

    private static final Map<Character, Type> MONO_OP = Stream.of(
            new SimpleEntry<>(',', Type.S_CM), new SimpleEntry<>('=', Type.S_EQ),
            new SimpleEntry<>('-', Type.S_MN), new SimpleEntry<>('+', Type.S_AD),
            new SimpleEntry<>('?', Type.S_QM), new SimpleEntry<>(':', Type.S_CO),
            new SimpleEntry<>('|', Type.S_OR), new SimpleEntry<>('~', Type.S_TD),
            new SimpleEntry<>('*', Type.S_ST), new SimpleEntry<>('/', Type.S_DV),
            new SimpleEntry<>('%', Type.S_MD), new SimpleEntry<>('!', Type.S_EX),
            new SimpleEntry<>('&', Type.S_AM), new SimpleEntry<>(';', Type.S_SM),
            new SimpleEntry<>('<', Type.S_LA), new SimpleEntry<>('>', Type.S_RA),
            new SimpleEntry<>('(', Type.S_LP), new SimpleEntry<>(')', Type.S_RP),
            new SimpleEntry<>('[', Type.S_LS), new SimpleEntry<>(']', Type.S_RS),
            new SimpleEntry<>('{', Type.S_LB), new SimpleEntry<>('}', Type.S_RB))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
 
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
                default:
                    return getTokenFrom(c).orElseGet(() -> {
                        unread(c);

                        // Using orElseGet instead of orElseThrow since Java 8
                        // cannot infer Throwable is BadCharException which is a RTE
                        return lexNumeric().orElseGet(() -> lexIdent().orElseGet(() -> {
                            throw new BadCharException(c);
                        }));
                    });
            }
        }
        return null;
    }

    private Optional<Token<Type>> getTokenFrom(final char c) {
        return Optional.ofNullable(MONO_OP.get(c))
                .map(type -> new Token<>(type, Character.toString(c)));
    }

    private Optional<Token<Type>> lexIdent() {
        final String i = readWhile(RsetLexer::isIdent);
        if (!i.isEmpty()) {
            return Optional.of(new Token<>(Type.L_IDENT, i));
        }
        return Optional.empty();
    }

    private Optional<Token<Type>> lexNumeric() {
        final String t = readWhile(Character::isDigit);
        final int k = read();
        if (k == '.') {
            final String frac = readWhile(Character::isDigit);
            return Optional.of(new Token<>(Type.L_REAL, t + '.' + (frac.isEmpty() ? '0' : frac)));
        } else {
            unread(k);
        }
        if (!t.isEmpty()) {
            return Optional.of(new Token<>(Type.L_INT, t));
        }
        return Optional.empty();
    }

    private static boolean processStrEsc(final int e, final StringBuilder sb) {
        if (e == -1) return false;

        final char c = (char) e;
        if (!STR_ESC.contains(c)) {
            sb.append('\\');
        }
        sb.append('\\').append(c);
        return true;
    }

    private boolean processStrChar(final char k, final StringBuilder sb) {
        if (k == '\\') {
            return processStrEsc(read(), sb);
        } else {
            if (k == '\'' || k == '\"') {
                // escape it, avoids problems
                sb.append('\\');
            }
            sb.append(k);
        }
        return true;
    }

    private Token<Type> lexRawString(final char quoteMark) {
        final StringBuilder sb = new StringBuilder();
        // unterminated strings are considered complete
        boolean run = true;
        while (run) {
            int k = read();
            if (k == -1 || k == quoteMark) {
                break;
            }

            run = processStrChar((char) k, sb);
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
        if (buf != -1) throw new BufferOverflowException();
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