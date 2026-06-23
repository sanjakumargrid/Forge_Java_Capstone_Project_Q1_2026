package com.talentgrid.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsAutoConfiguration {

    public CorsAutoConfiguration() {
        System.out.println("===== GLOBAL CORS CONFIG LOADED =====");
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                System.out.println("===== REGISTERING GLOBAL CORS =====");

                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:4200", "http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
