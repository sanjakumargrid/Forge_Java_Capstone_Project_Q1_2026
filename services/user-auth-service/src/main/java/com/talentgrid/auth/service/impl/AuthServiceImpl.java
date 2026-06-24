package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.request.LoginRequest;
import com.talentgrid.auth.dto.request.RegisterRequest;
import com.talentgrid.auth.dto.response.LoginResponse;
import com.talentgrid.auth.dto.response.RegisterResponse;
import com.talentgrid.auth.entity.RefreshToken;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.jwt.JwtBlacklistService;
import com.talentgrid.auth.jwt.JwtService;
import com.talentgrid.auth.kafka.UserCreatedEventPublisher;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.oauth.OAuthAuthorizationCodeService;
import com.talentgrid.auth.service.RefreshTokenService;
import com.talentgrid.auth.service.interfaces.AuthService;
import com.talentgrid.auth.service.interfaces.UserSecurityCacheService;
import com.talentgrid.kafka.events.auth.UserCreatedPayload;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core business logic implementation for user authentication.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Handle user registration and login</li>
 *   <li>Enforce account locking after multiple failed attempts</li>
 *   <li>Manage the lifecycle of access tokens and refresh tokens</li>
 *   <li>Integrate with Redis caching for user context</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final AuthenticationManager authenticationManager;
    private final UserSecurityCacheService userSecurityCacheService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtBlacklistService jwtBlacklistService;
    private final UserCreatedEventPublisher userCreatedEventPublisher;
    private final OAuthAuthorizationCodeService oauthAuthorizationCodeService;


    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        String requestedRole =
                request.getRole() != null ? request.getRole() : "EMPLOYEE";
        Role.requireAllowedName(requestedRole);

        Role role = roleRepository.findByName(requestedRole.toUpperCase())
                .orElseThrow(() ->
                        new RuntimeException("Role not found: " + requestedRole)
                );

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .location(request.getLocation())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .slackId(request.getSlackId())
                .roles(Set.of(role))
                .failedAttempts(0)
                .accountLocked(false)
                .build();

        User savedUser = userRepository.save(user);

        UserCreatedPayload payload = UserCreatedPayload.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRoles().stream()
                        .map(Role::getName)
                        .findFirst()
                        .orElse("EMPLOYEE"))
                .location(savedUser.getLocation())
                .source("SELF_REGISTER")
                .build();

        userCreatedEventPublisher.publishUserCreated(payload);

        return RegisterResponse.builder()
                .message("User registered successfully")
                .username(user.getUsername())
                .email(user.getEmail())
                .role(role.getName())
                .build();
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        // ==============================
        // ENABLED CHECK
        // ==============================
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new RuntimeException("Account disabled. Please contact administrator.");
        }

        // ==============================
        // ACCOUNT LOCK CHECK
        // ==============================
        if (Boolean.TRUE.equals(user.getAccountLocked())) {

            if (user.getLockTime() != null &&
                    user.getLockTime()
                            .plusMinutes(LOCK_DURATION_MINUTES)
                            .isBefore(LocalDateTime.now())) {

                user.setAccountLocked(false);
                user.setFailedAttempts(0);
                user.setLockTime(null);

                userRepository.save(user);

            } else {
                throw new RuntimeException("Account locked. Try again later.");
            }
        }

        // ==============================
        // PASSWORD VALIDATION
        // ==============================
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            int attempts = user.getFailedAttempts() == null ? 0 : user.getFailedAttempts();
            attempts++;

            user.setFailedAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
            }

            userRepository.save(user);

            throw new BadCredentialsException("Invalid email or password");
        }

        // ==============================
        // SUCCESS LOGIN RESET
        // ==============================
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
        userRepository.save(user);

        userSecurityCacheService.cacheUser(user);

        // ==============================
        // JWT + REFRESH TOKEN
        // ==============================
        String accessToken = jwtService.generateToken(user);

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", refreshToken.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(jwtService.getRefreshExpiration() / 1000)
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .type("Bearer")
                .email(user.getEmail())
                .roles(user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(
            String refreshToken,
            HttpServletResponse response
    ) {

        // validate old refresh token
        RefreshToken storedToken =
                refreshTokenService.validateRefreshToken(refreshToken);

        User user = storedToken.getUser();

        // Check if user got disabled or locked while refresh token was alive
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new RuntimeException("Account disabled. Please contact administrator.");
        }
        
        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new RuntimeException("Account locked. Try again later.");
        }

        // Ensure user is re-cached to keep TTL alive
        userSecurityCacheService.cacheUser(user);

        // ROTATE TOKEN
        RefreshToken newRefreshToken =
                refreshTokenService.rotateRefreshToken(storedToken);

        // generate new access token
        String accessToken = jwtService.generateToken(user);

        // create NEW cookie
        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", newRefreshToken.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(jwtService.getRefreshExpiration() / 1000)
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .type("Bearer")
                .email(user.getEmail())
                .roles(user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    @Transactional
    public LoginResponse exchangeOAuthCode(String code, HttpServletResponse response) {
        Long userId = oauthAuthorizationCodeService.consumeCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid or expired OAuth code"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new RuntimeException("Account disabled. Please contact administrator.");
        }

        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new RuntimeException("Account locked. Try again later.");
        }

        userSecurityCacheService.cacheUser(user);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", refreshToken.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(jwtService.getRefreshExpiration() / 1000)
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .type("Bearer")
                .email(user.getEmail())
                .roles(user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

}