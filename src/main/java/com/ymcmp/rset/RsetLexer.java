package com.ymcmp.rset;

import java.io.Reader;
import java.io.Closeable;
import java.io.IOException;

import com.ymcmp.lexparse.Lexer;
import com.ymcmp.lexparse.Token;

public class RsetLexer implements Lexer<Type>, Closeable {

    private Reader reader;
    private int buf = -1;

    public RsetLexer(final Reader reader) {
        this.reader = reader;
    }

    @Override
    public Token<Type> nextToken() {
        while (true) {
            final int c = read();
            switch (c) {
                case -1:   return null;
                case '#':  readWhile(k -> !Lexer.isEOL(k));
                case ' ':
                case '\n':
                case '\r':
                case '\t': continue;
// NOTE: Above cases abuses fallthroughs! Careful when re-ordering
                case ',':  return new Token(Type.S_CM, ",");
                case '=':  return new Token(Type.S_EQ, "=");
                case '-':  return new Token(Type.S_MN, "-");
                case '+':  return new Token(Type.S_AD, "+");
                case '?':  return new Token(Type.S_QM, "?");
                case ':':  return new Token(Type.S_CO, ":");
                case '|':  return new Token(Type.S_OR, "|");
                case '~':  return new Token(Type.S_TD, "~");
                case '*':  return new Token(Type.S_ST, "*");
                case '/':  return new Token(Type.S_DV, "/");
                case '%':  return new Token(Type.S_MD, "&");
                case '!':  return new Token(Type.S_EX, "!");
                case '&':  return new Token(Type.S_AM, "&");
                case '(':  return new Token(Type.S_LP, "(");
                case ')':  return new Token(Type.S_RP, ")");
                case '{':  return new Token(Type.S_LB, "{");
                case '}':  return new Token(Type.S_RB, "}");
                case '\'':
                case '"': {
                    final StringBuilder sb = new StringBuilder();
                feedback: while (true) {
                        final int k = read();
                        switch (k) {
                            case -1:  // Unterminated strings are considered complete
                                break feedback;
                            case '\\': {    // Escape sequences
                                final int e = read();
                                switch (e) {
                                    case '\\':
                                    case '\'':
                                    case '"': sb.append((char) e);
                                    case 'a': sb.append('\u0007');
                                    case 'b': sb.append('\b');
                                    case 't': sb.append('\t');
                                    case 'n': sb.append('\n');
                                    case 'v': sb.append('\u000b');
                                    case 'f': sb.append('\f');
                                    case 'r': sb.append('\r');
                                    default: sb.append((char) k).append((char) e);
                                }
                                break;
                            } 
                            default:
                                if (k == c) break feedback; // Terminating mark
                                sb.append((char) k);
                        }
                    }
                    return new Token(Type.L_IDENT, sb.toString());
                }
                default: {
                    unread(c);

                    final String t = readWhile(Character::isDigit);
                    if (!t.isEmpty()) {
                        return new Token(Type.L_NUMBER, t);
                    }

                    final String i = readWhile(RsetLexer::isIdent);
                    if (!i.isEmpty()) {
                        return new Token(Type.L_IDENT, i);
                    }

                    throw new RuntimeException("Unknown char " + (char) c);
                }
            }
        }
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