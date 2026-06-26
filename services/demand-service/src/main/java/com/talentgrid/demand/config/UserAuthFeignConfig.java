package com.talentgrid.demand.config;

import com.talentgrid.shared.auth.jwt.JwtTokenService;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign configuration ONLY for UserAuthServiceClient.
 *
 * <p>Propagates the incoming JWT Authorization header to outbound
 * inter-service calls to user-auth-service. When no request context exists
 * (e.g. scheduled jobs), falls back to an internal service token.
 */
public class UserAuthFeignConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Bean
    public RequestInterceptor jwtPropagationInterceptor(JwtTokenService jwtTokenService) {
        return requestTemplate -> {
            String token = null;
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader(AUTHORIZATION_HEADER);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
            if (token == null) {
                token = jwtTokenService.generateInternalServiceToken();
            }
            requestTemplate.header(AUTHORIZATION_HEADER, "Bearer " + token);
        };
    }
}
