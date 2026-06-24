package com.talentgrid.auth.oauth;

import com.talentgrid.auth.config.OAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Redirects Google OAuth failures to the configured frontend login page.
 */
@Component
@RequiredArgsConstructor
public class GoogleOAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuthProperties oauthProperties;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        String redirectUrl = oauthProperties.getFrontendBaseUrl()
                + oauthProperties.getLoginErrorPath()
                + "?error="
                + URLEncoder.encode("oauth_failed", StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
