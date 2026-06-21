package com.talentgrid.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties binding for JWT-related settings.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Bind {@code jwt.*} properties from {@code application.properties}</li>
 *   <li>Provide strongly-typed access to JWT configuration values</li>
 * </ul>
 *
 * <p>Currently configured properties:
 * <ul>
 *   <li>{@code jwt.secret}: The HMAC-SHA secret key used for signing and verifying JWTs</li>
 * </ul>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * The cryptographic secret key used for JWT signature generation and verification.
     * Must be sufficiently long and secure (e.g., 256-bit for HS256).
     */
    private String secret;
}