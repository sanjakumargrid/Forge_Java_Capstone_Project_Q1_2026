package com.talentgrid.shared.config;

import com.talentgrid.shared.auth.jwt.JwtTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAutoConfiguration {

    @Bean
    public JwtTokenService jwtTokenService(
            JwtProperties properties
    ) {

        return new JwtTokenService(
                properties.getSecret()
        );
    }
}