package com.talentgrid.auth.oauth;

import com.talentgrid.auth.config.OAuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads and writes the HttpOnly OAuth exchange-code cookie used between
 * Google login success and {@code POST /api/v1/auth/oauth/token}.
 */
@Component
@RequiredArgsConstructor
public class OAuthExchangeCodeCookieSupport {

    private final OAuthProperties oauthProperties;

    public void writeExchangeCodeCookie(HttpServletResponse response, String exchangeCode) {
        ResponseCookie cookie = ResponseCookie
                .from(oauthProperties.getExchangeCodeCookieName(), exchangeCode)
                .httpOnly(true)
                .secure(oauthProperties.isExchangeCodeCookieSecure())
                .path(oauthProperties.getExchangeCodeCookiePath())
                .sameSite(oauthProperties.getExchangeCodeCookieSameSite())
                .maxAge(oauthProperties.getExchangeCodeTtlSeconds())
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public Optional<String> readExchangeCodeCookie(HttpServletRequest request) {
        Optional<String> fromServletCookies = readFromServletCookies(request);
        if (fromServletCookies.isPresent()) {
            return fromServletCookies;
        }

        return readFromCookieHeader(request.getHeader("Cookie"));
    }

    private Optional<String> readFromServletCookies(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        for (Cookie cookie : request.getCookies()) {
            if (oauthProperties.getExchangeCodeCookieName().equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return Optional.of(value);
                }
            }
        }

        return Optional.empty();
    }

    private Optional<String> readFromCookieHeader(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return Optional.empty();
        }

        String cookieName = oauthProperties.getExchangeCodeCookieName() + "=";
        for (String part : cookieHeader.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(cookieName)) {
                String value = trimmed.substring(cookieName.length());
                if (!value.isBlank()) {
                    return Optional.of(value);
                }
            }
        }

        return Optional.empty();
    }

    public void clearExchangeCodeCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from(oauthProperties.getExchangeCodeCookieName(), "")
                .httpOnly(true)
                .secure(oauthProperties.isExchangeCodeCookieSecure())
                .path(oauthProperties.getExchangeCodeCookiePath())
                .maxAge(0)
                .sameSite(oauthProperties.getExchangeCodeCookieSameSite())
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
