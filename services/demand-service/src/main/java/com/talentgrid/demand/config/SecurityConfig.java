package com.talentgrid.demand.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Global security configuration for the Demand Service.
 *
 * <p>Currently configures a permissive security filter chain (disabling CSRF and allowing all requests)
 * to facilitate local development and initial integration testing. 
 * <p>Note: Method-level security (@PreAuthorize) and strict JWT validation will be re-enabled 
 * once the shared auth-client library is fully integrated.
 */
@Configuration
@EnableWebSecurity
// TODO: Re-enable once JWT token validation library is integrated (Phase 1, Week 1 Day 3)
// @EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
