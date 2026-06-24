package com.talentgrid.auth.oauth;

import lombok.Getter;

/**
 * Business validation failure during Google OAuth login.
 */
@Getter
public class OAuthLoginException extends RuntimeException {

    private final String errorCode;

    public OAuthLoginException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
