package com.talentgrid.demand.config;

import com.talentgrid.shared.auth.jwt.JwtTokenService;
import com.talentgrid.shared.auth.security.JwtAuthenticationProvider;
import com.talentgrid.shared.config.JwtAutoConfiguration;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JwtAutoConfiguration.class)
public class JwtConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void debugSecret() {
        System.out.println(">>> JWT SECRET LOADED: [" + jwtSecret + "]");
    }

    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationProvider(jwtTokenService);
    }
}