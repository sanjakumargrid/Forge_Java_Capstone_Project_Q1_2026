package com.talentgrid.auth.config;

import com.talentgrid.auth.filter.JwtAuthenticationFilter;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.jwt.JwtService;
import com.talentgrid.auth.service.interfaces.UserSecurityCacheService;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.entity.Role;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;


/**
 * Core Spring Security configuration for the auth-service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Configure stateless JWT-based session management for API endpoints</li>
 *   <li>Configure OAuth2 login with session support for browser-based SSO flows</li>
 *   <li>Define CORS policies</li>
 *   <li>Register the {@link JwtAuthenticationFilter} in the filter chain</li>
 * </ul>
 *
 * <p>Note: The dual session creation policy (STATELESS globally, but using
 * {@link HttpSession} during OAuth2 login) is an intentional design choice
 * to support the OAuth2 callback flow while keeping API endpoints stateless.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final UserSecurityCacheService userSecurityCacheService;

    /**
     * Configures the main security filter chain.
     *
     * @param http the HttpSecurity builder
     * @return the configured security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(cors -> {})

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/**",
                                "/oauth2/**",
                                "/login/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        )
                        .permitAll()

                        .anyRequest()
                        .authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                (request, response, authException) -> {

                                    response.setStatus(
                                            HttpServletResponse.SC_UNAUTHORIZED
                                    );

                                    response.setContentType(
                                            "application/json"
                                    );

                                    response.getWriter().write("""
                                            {
                                              "error": "Unauthorized"
                                            }
                                            """);
                                }
                        )
                )

                // GOOGLE LOGIN
                .oauth2Login(oauth -> oauth
                        .successHandler((request, response, authentication) -> {

                            OAuth2User oauthUser =
                                    (OAuth2User) authentication.getPrincipal();

                            String email =
                                    oauthUser.getAttribute("email");

                            Boolean emailVerified =
                                    oauthUser.getAttribute("email_verified");

                            if (email == null || email.isBlank()) {

                                response.sendRedirect(
                                        "http://localhost:4200/login?error=email_missing"
                                );
                                return;
                            }

                            if (!Boolean.TRUE.equals(emailVerified)) {

                                response.sendRedirect(
                                        "http://localhost:4200/login?error=email_not_verified"
                                );
                                return;
                            }

                            if (!email.toLowerCase()
                                    .endsWith("@griddynamics.com")) {

                                response.sendRedirect(
                                        "http://localhost:4200/login?error=unauthorized_domain"
                                );
                                return;
                            }

                            User user = userRepository.findByEmail(email).orElse(null);
                            if (user == null) {
                                Role role = roleRepository.findByName("EMPLOYEE").orElseThrow(() -> new RuntimeException("Role not found"));
                                user = User.builder()
                                        .username(email.substring(0, email.indexOf('@')))
                                        .email(email)
                                        .password("")
                                        .enabled(true)
                                        .roles(Set.of(role))
                                        .build();
                                userRepository.save(user);
                            }

                            userSecurityCacheService.cacheUser(user);
                            String token = jwtService.generateToken(user);
                            
                            response.sendRedirect(
                                    "http://localhost:4200/auth/callback?token=" + token
                            );
                        })
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Provides a BCrypt password encoder for hashing and verifying passwords.
     *
     * @return the configured password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the AuthenticationManager bean used by the AuthController.
     *
     * @param config the authentication configuration
     * @return the authentication manager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {

        return config.getAuthenticationManager();
    }

    /**
     * Configures CORS policies for local frontend development environments.
     *
     * @return the configured CORS source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:3000",
                        "http://localhost:4200"
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}