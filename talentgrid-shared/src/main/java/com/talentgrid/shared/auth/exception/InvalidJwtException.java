package com.talentgrid.shared.auth.exception;

public class InvalidJwtException
        extends RuntimeException {

    public InvalidJwtException(String message) {
        super(message);
    }
}