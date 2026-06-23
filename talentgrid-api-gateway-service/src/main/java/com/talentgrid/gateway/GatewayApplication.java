package com.talentgrid.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.talentgrid.gateway.config.GatewayRuntimeProperties;
import com.talentgrid.gateway.config.RbacProperties;

/**
 * The main entry point for the TalentGrid API Gateway Service.
 * 
 * <p>This Spring Boot application acts as the single point of ingress for the Forge
 * platform, responsible for routing requests, enforcing stateless JWT authentication,
 * applying Role-Based Access Control (RBAC), and managing distributed rate limits.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties({ RbacProperties.class, GatewayRuntimeProperties.class })
public class GatewayApplication {

    /**
     * Bootstraps the reactive Spring Cloud Gateway application.
     *
     * @param args command-line arguments passed during startup
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
