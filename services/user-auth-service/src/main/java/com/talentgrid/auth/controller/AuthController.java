package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.request.RegisterRequest;
import com.talentgrid.auth.dto.response.RegisterResponse;
import com.talentgrid.auth.dto.request.LoginRequest;
import com.talentgrid.auth.dto.response.LoginResponse;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.UserRepository;
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
import com.talentgrid.auth.jwt.JwtBlacklistService;
import com.talentgrid.auth.jwt.JwtService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtBlacklistService jwtBlacklistService;

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        System.out.println("REGISTER API HIT");

        RegisterResponse response =
                authService.register(request);

        return ResponseEntity.ok(response);
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
        if (authentication != null) {

            String email = authentication.getName();

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