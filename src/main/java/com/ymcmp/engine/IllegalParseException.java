package com.ymcmp.engine;

public class IllegalParseException extends RuntimeException {

    public IllegalParseException(String message) {
        super(message);
    }

    public IllegalParseException(String message, Throwable trace) {
        super(message, trace);
    }
}