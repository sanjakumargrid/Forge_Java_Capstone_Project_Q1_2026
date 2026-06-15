package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.response.LoginResponse;
import com.talentgrid.auth.entity.RefreshToken;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.jwt.JwtService;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.RefreshTokenService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuthController {

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final RefreshTokenService refreshTokenService;

    @Deprecated
    @GetMapping("/oauth-success")
    public ResponseEntity<LoginResponse> oauthSuccess(
            @RequestParam String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {

                    // DEFAULT ROLE
                    Role employeeRole =
                            roleRepository
                                    .findByName("EMPLOYEE")
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "EMPLOYEE role not found"
                                            )
                                    );

                    User newUser = User.builder()
                            .username(email.split("@")[0])
                            .email(email)
                            .password("OAUTH_USER")
                            .enabled(true)
                            .roles(Set.of(employeeRole))
                            .build();

                    return userRepository.save(newUser);
                });

        String accessToken =
                jwtService.generateToken(user);

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(
                LoginResponse.builder()
                        .accessToken(accessToken)
                        .type("Bearer")
                        .email(user.getEmail())
                        .roles(
                                user.getRoles()
                                        .stream()
                                        .map(Role::getName)
                                        .collect(Collectors.toSet())
                        )
                        .build()
        );
    }

    @GetMapping("/session")
    public ResponseEntity<LoginResponse> session(
            HttpSession session
    ) {

        String email =
                (String) session.getAttribute(
                        "oauth_email"
                );

        Boolean authenticated =
                (Boolean) session.getAttribute(
                        "oauth_authenticated"
                );

        if (email == null ||
                !Boolean.TRUE.equals(authenticated)) {

            throw new RuntimeException(
                    "OAuth authentication required"
            );
        }

        session.removeAttribute("oauth_email");
        session.removeAttribute("oauth_authenticated");

        return oauthSuccess(email);
    }

}
