package com.rafptor.parser.exception;

public class MalformedFieldException extends AfpParseException {

    public MalformedFieldException(String message) {
        super(message);
    }

    public MalformedFieldException(String message, Throwable cause) {
        super(message, cause);
    }
}
