package com.talentgrid.auth.controller;

import com.talentgrid.auth.config.OAuthProperties;
import com.talentgrid.auth.oauth.OAuthBridgeTokenService;
import com.talentgrid.auth.oauth.OAuthExchangeCodeCookieSupport;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Sets the HttpOnly OAuth exchange cookie on a dedicated gateway-origin request.
 *
 * <p>After Google login, the success handler redirects here (via the API Gateway) so
 * {@code Set-Cookie} is applied reliably before the browser is sent to the SPA callback.
 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
public class OAuthCookieBridgeController {

    private final OAuthBridgeTokenService oauthBridgeTokenService;
    private final OAuthExchangeCodeCookieSupport oauthExchangeCodeCookieSupport;
    private final OAuthProperties oauthProperties;

    @GetMapping("/bridge/{bridgeToken}")
    public void establishExchangeCookie(
            @PathVariable String bridgeToken,
            HttpServletResponse response
    ) throws IOException {
        String exchangeCode = oauthBridgeTokenService.consumeBridgeToken(bridgeToken)
                .orElseThrow(() -> new RuntimeException("Invalid or expired OAuth bridge token"));

        oauthExchangeCodeCookieSupport.writeExchangeCodeCookie(response, exchangeCode);

        response.sendRedirect(
                oauthProperties.getFrontendBaseUrl() + oauthProperties.getCallbackPath()
        );
    }
}
