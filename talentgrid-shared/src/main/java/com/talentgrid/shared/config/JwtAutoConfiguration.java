package com.talentgrid.shared.config;

import com.talentgrid.shared.auth.jwt.JwtTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot AutoConfiguration for the shared JWT infrastructure.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Automatically configure shared JWT beans when this module is included</li>
 *   <li>Bind JWT properties from {@code application.properties}</li>
 *   <li>Provide a singleton {@link JwtTokenService} to consuming microservices</li>
 * </ul>
 *
 * <p>This configuration is registered in {@code META-INF/spring.factories}
 * and {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * to ensure automatic loading by Spring Boot's auto-configuration mechanism
 * across all TalentGrid microservices.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAutoConfiguration {

    /**
     * Provisions the shared {@link JwtTokenService} bean used for
     * parsing and validating JWTs across all microservices.
     *
     * @param properties the bound JWT properties containing the cryptographic secret
     * @return a configured {@link JwtTokenService} singleton
     */
    @Bean
    public JwtTokenService jwtTokenService(
            JwtProperties properties
    ) {

        return new JwtTokenService(
                properties.getSecret()
        );
    }
}