package com.talentgrid.workforce.common.config;

import com.talentgrid.shared.auth.jwt.JwtTokenService;
import com.talentgrid.shared.auth.security.JwtAuthenticationProvider;
import com.talentgrid.shared.config.JwtAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!dev")
@Import(JwtAutoConfiguration.class)
public class JwtConfig {

    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationProvider(jwtTokenService);
    }
}
