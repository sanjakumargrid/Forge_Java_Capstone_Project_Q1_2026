package com.talentgrid.jobposting.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobPostingOpenAPI() {
        final String bearer = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Job Posting Service API")
                        .description("RMS Job Posting microservice — demands, job postings and recruiter tooling.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(bearer, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
