package com.talentgrid.auth.oauth;

import com.talentgrid.auth.config.OAuthProperties;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.kafka.UserCreatedEventPublisher;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.interfaces.UserSecurityCacheService;
import com.talentgrid.kafka.events.auth.UserCreatedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Resolves or provisions local users from a verified Google {@link OAuth2User}.
 */
@Service
@RequiredArgsConstructor
public class GoogleOAuthLoginService {

    private static final String EMPLOYEE_ROLE = "EMPLOYEE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSecurityCacheService userSecurityCacheService;
    private final UserCreatedEventPublisher userCreatedEventPublisher;
    private final OAuthProperties oauthProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Loads an existing user or creates a new EMPLOYEE account for a Google identity.
     *
     * @param oauthUser authenticated Google principal
     * @return active local user
     * @throws OAuthLoginException when the account cannot be used
     */
    @Transactional
    public User resolveUser(OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        Boolean emailVerified = oauthUser.getAttribute("email_verified");

        if (email == null || email.isBlank()) {
            throw new OAuthLoginException("email_missing", "Google account email is missing");
        }

        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new OAuthLoginException("email_not_verified", "Google account email is not verified");
        }

        if (!isAllowedDomain(email)) {
            throw new OAuthLoginException("unauthorized_domain", "Email domain is not authorized");
        }

        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            assertAccountUsable(existing);
            userSecurityCacheService.cacheUser(existing);
            return existing;
        }

        Role employeeRole = roleRepository.findByName(EMPLOYEE_ROLE)
                .orElseThrow(() -> new OAuthLoginException(
                        "role_not_found",
                        "Default role not found: " + EMPLOYEE_ROLE
                ));

        String username = email.substring(0, email.indexOf('@'));
        User created = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(generateUnusablePassword()))
                .enabled(true)
                .roles(Set.of(employeeRole))
                .failedAttempts(0)
                .accountLocked(false)
                .build();

        User savedUser = userRepository.save(created);
        userSecurityCacheService.cacheUser(savedUser);

        UserCreatedPayload payload = UserCreatedPayload.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(EMPLOYEE_ROLE)
                .location(savedUser.getLocation())
                .source("GOOGLE_OAUTH")
                .build();
        userCreatedEventPublisher.publishUserCreated(payload);

        return savedUser;
    }

    private void assertAccountUsable(User user) {
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new OAuthLoginException("account_disabled", "Account is disabled");
        }

        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new OAuthLoginException("account_locked", "Account is locked");
        }
    }

    private boolean isAllowedDomain(String email) {
        return email.toLowerCase().endsWith(oauthProperties.getAllowedEmailDomain().toLowerCase());
    }

    private String generateUnusablePassword() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
