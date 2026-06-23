package com.talentgrid.demand.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign configuration ONLY for UserAuthServiceClient.
 *
 * <p>Propagates the incoming JWT Authorization header to outbound
 * inter-service calls to user-auth-service. This config is NOT
 * registered as a global @Configuration — it is referenced explicitly
 * in the @FeignClient(configuration = ...) annotation so it does NOT
 * apply to GeminiAiClient or any other Feign client.
 */
public class UserAuthFeignConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Bean
    public RequestInterceptor jwtPropagationInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader(AUTHORIZATION_HEADER);
                if (authHeader != null) {
                    requestTemplate.header(AUTHORIZATION_HEADER, authHeader);
                }
            }
        };
    }
}
