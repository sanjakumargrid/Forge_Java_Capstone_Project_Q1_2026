package com.talentgrid.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Global security configuration for the WebFlux-based API Gateway.
 * 
 * <p>This configuration disables Spring Security's default behaviors (such as form-login, 
 * HTTP Basic authentication, and CSRF protection) because the Gateway implements a custom,
 * stateless, token-based security architecture using {@code JwtAuthenticationFilter} and 
 * {@code ScopeAuthorizationManager}.</p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configures the {@link SecurityWebFilterChain} for the gateway.
     * 
     * <p>Note: {@code permitAll()} is used here to bypass Spring Security's default 
     * authorization checks. This does <strong>not</strong> mean endpoints are unprotected;
     * rather, it delegates the responsibility entirely to our custom global filters.</p>
     *
     * @param http the {@link ServerHttpSecurity} object to configure
     * @return the configured {@link SecurityWebFilterChain}
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )
                .build();
    }
}
