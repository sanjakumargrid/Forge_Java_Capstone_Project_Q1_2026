package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.request.LoginRequest;
import com.talentgrid.auth.dto.request.OAuthTokenExchangeRequest;
import com.talentgrid.auth.dto.request.RegisterRequest;
import com.talentgrid.auth.dto.response.LoginResponse;
import com.talentgrid.auth.dto.response.RegisterResponse;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.jwt.JwtBlacklistService;
import com.talentgrid.auth.jwt.JwtService;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.security.CachedUserPrincipal;
import com.talentgrid.auth.service.RefreshTokenService;
import com.talentgrid.auth.service.interfaces.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for handling user authentication endpoints.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Expose endpoints for registration, login, token refresh, and logout</li>
 *   <li>Manage HTTP-only secure cookies for refresh tokens</li>
 *   <li>Integrate with authentication and JWT services</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtBlacklistService jwtBlacklistService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Registers a new user.
     *
     * @param request the registration details
     * @return response indicating success and assigned role
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        RegisterResponse response =
                authService.register(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Authenticates a user and issues JWT and refresh tokens.
     *
     * @param request  the login credentials
     * @param response HTTP response to attach the refresh token cookie
     * @return response containing the JWT access token and user roles
     */
    /**
     * Exchanges a one-time Google OAuth code for JWT and refresh tokens.
     *
     * <p>Called by the SPA after the browser redirect from {@code /login/oauth2/code/google}.
     */
    @PostMapping("/oauth/token")
    public ResponseEntity<LoginResponse> exchangeOAuthToken(
            @Valid @RequestBody OAuthTokenExchangeRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.exchangeOAuthCode(request.getCode(), response));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {

        LoginResponse loginResponse =
                authService.login(request, response);

        return ResponseEntity.ok(loginResponse);
    }

    /**
     * Issues a new JWT access token using a valid refresh token.
     *
     * @param request  HTTP request to extract the refresh token cookie
     * @param response HTTP response to attach the newly rotated refresh token cookie
     * @return response containing the new JWT access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        String refreshToken = null;

        if (request.getCookies() != null) {

            for (Cookie cookie : request.getCookies()) {

                if ("refreshToken".equals(cookie.getName())) {

                    refreshToken = cookie.getValue();
                }
            }
        }

        if (refreshToken == null) {

            throw new RuntimeException("Refresh token missing");
        }

        LoginResponse loginResponse =
                authService.refreshToken(refreshToken, response);

        return ResponseEntity.ok(loginResponse);
    }

    /**
     * Logs out the user by revoking their refresh token and blacklisting
     * their current access token.
     *
     * @param request        HTTP request to extract the JWT from the Authorization header
     * @param response       HTTP response to clear the refresh token cookie
     * @param authentication the current Spring Security authentication
     * @return success message
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {

        // =========================================
        // EXTRACT JWT FROM AUTHORIZATION HEADER
        // =========================================
        String authHeader =
                request.getHeader("Authorization");

        String jwt = null;

        if (authHeader != null &&
                authHeader.startsWith("Bearer ")) {

            jwt = authHeader.substring(7);
        }

        // =========================================
        // REVOKE REFRESH TOKEN + BLACKLIST JWT
        // =========================================
        if (authentication != null && authentication.getPrincipal() instanceof CachedUserPrincipal) {

            CachedUserPrincipal principal = (CachedUserPrincipal) authentication.getPrincipal();
            String email = principal.getEmail();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new RuntimeException("User not found")
                    );

            // Existing refresh token revoke logic
            refreshTokenService.revokeRefreshToken(user);

            // =========================================
            // BLACKLIST ACCESS JWT IN REDIS
            // =========================================
            if (jwt != null) {

                long remainingTime =
                        jwtService.extractExpiration(jwt)
                                .getTime()
                                - System.currentTimeMillis();

                String jti = jwtService.extractJti(jwt);

                jwtBlacklistService.blacklistToken(
                        jti,
                        remainingTime
                );
            }
        }

        // =========================================
        // DELETE REFRESH TOKEN COOKIE
        // =========================================
        ResponseCookie deleteCookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(
                "Set-Cookie",
                deleteCookie.toString()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logged out successfully"
                )
        );
    }
}