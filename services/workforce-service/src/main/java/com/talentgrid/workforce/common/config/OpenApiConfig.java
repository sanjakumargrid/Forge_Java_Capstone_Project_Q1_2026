package com.talentgrid.workforce.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Workforce Service API",
                version = "v1",
                description = "OpenAPI documentation for Workforce Service"
        )
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi workforceApi() {
        return GroupedOpenApi.builder()
                .group("workforce-service")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
