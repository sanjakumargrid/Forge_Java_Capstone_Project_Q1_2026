package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.dto.request.LoginRequest;
import com.talentgrid.auth.dto.request.RegisterRequest;
import com.talentgrid.auth.dto.response.LoginResponse;
import com.talentgrid.auth.dto.response.RegisterResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Service interface for core authentication business logic.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Handle user registration and login</li>
 *   <li>Manage access tokens and refresh tokens</li>
 * </ul>
 */
public interface AuthService {

    /**
     * Authenticates a user and generates tokens.
     *
     * @param request  the login credentials
     * @param response the HTTP response (to attach the refresh token cookie)
     * @return the login response containing the access token
     */
    LoginResponse login(
            LoginRequest request,
            HttpServletResponse response
    );

    /**
     * Issues a new access token using a valid refresh token.
     *
     * @param refreshToken the valid refresh token string
     * @param response     the HTTP response (to attach the rotated refresh token cookie)
     * @return the login response containing the new access token
     */
    LoginResponse refreshToken(String refreshToken, HttpServletResponse response);

    /**
     * Registers a new user.
     *
     * @param request the registration details
     * @return the registration response
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Exchanges a one-time Google OAuth code for JWT and refresh tokens.
     *
     * @param code     short-lived code from the OAuth browser redirect
     * @param response HTTP response used to attach the refresh token cookie
     * @return login response containing the access token
     */
    LoginResponse exchangeOAuthCode(String code, HttpServletResponse response);
}