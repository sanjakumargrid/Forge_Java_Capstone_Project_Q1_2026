package com.talentgrid.auth.oauth;

import com.talentgrid.auth.config.OAuthProperties;
import com.talentgrid.auth.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles successful Google OAuth2 login by issuing a one-time exchange code redirect.
 *
 * <p>JWTs are never placed in the browser URL. The SPA exchanges the code via
 * {@code POST /api/v1/auth/oauth/token}.
 */
@Component
@RequiredArgsConstructor
public class GoogleOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthProperties oauthProperties;
    private final GoogleOAuthLoginService googleOAuthLoginService;
    private final OAuthAuthorizationCodeService oauthAuthorizationCodeService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        try {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            User user = googleOAuthLoginService.resolveUser(oauthUser);
            String exchangeCode = oauthAuthorizationCodeService.createCode(
                    user.getId(),
                    oauthProperties.getExchangeCodeTtlSeconds()
            );

            String redirectUrl = UriComponentsBuilder
                    .fromUriString(oauthProperties.getFrontendBaseUrl())
                    .path(oauthProperties.getCallbackPath())
                    .queryParam("code", exchangeCode)
                    .build()
                    .toUriString();

            response.sendRedirect(redirectUrl);
        } catch (OAuthLoginException ex) {
            redirectToLoginError(response, ex.getErrorCode());
        }
    }

    private void redirectToLoginError(HttpServletResponse response, String errorCode) throws IOException {
        String redirectUrl = oauthProperties.getFrontendBaseUrl()
                + oauthProperties.getLoginErrorPath()
                + "?error="
                + URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }
}
