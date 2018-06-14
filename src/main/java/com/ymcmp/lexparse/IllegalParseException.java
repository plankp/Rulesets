package com.ymcmp.lexparse;

public class IllegalParseException extends RuntimeException {

    public IllegalParseException(Token<?> t) {
        super("Unexpected " + t);
    }

    public IllegalParseException(String message) {
        super(message);
    }

    public IllegalParseException(String message, Token<?> t) {
        super(message + (t == null ? "" : " (Found " + t + ")"));
    }
}