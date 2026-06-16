package com.talentgrid.application;

import com.talentgrid.application.integration.GeminiProperties;
import com.talentgrid.application.integration.OpenAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(
        scanBasePackages = {
                "com.talentgrid.application",
                "com.talentgrid.kafka",
                "com.talentgrid.audit"
        }
)
@EnableConfigurationProperties(GeminiProperties.class)
public class ApplicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                ApplicationServiceApplication.class,
                args
        );
    }
}