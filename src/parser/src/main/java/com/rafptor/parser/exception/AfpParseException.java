package com.rafptor.parser.exception;

public class AfpParseException extends RuntimeException {

    public AfpParseException(String message) {
        super(message);
    }

    public AfpParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
