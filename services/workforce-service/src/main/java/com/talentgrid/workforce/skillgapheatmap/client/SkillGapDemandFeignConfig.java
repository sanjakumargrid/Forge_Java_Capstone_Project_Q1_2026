package com.talentgrid.workforce.skillgapheatmap.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * Forwards the authenticated user's JWT to demand-service.
 * Tokens are taken from the incoming HTTP request, so they stay fresh for the
 * duration of the user's session — no static service token is required.
 */
public class SkillGapDemandFeignConfig {

    @Bean
    public RequestInterceptor skillGapDemandAuthInterceptor() {
        return template -> {
            String authorization = SkillGapAuthContext.resolveAuthorizationHeader();
            if (authorization != null) {
                template.header("Authorization", authorization);
            }
        };
    }
}
