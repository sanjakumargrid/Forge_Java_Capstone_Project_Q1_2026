package com.talentgrid.demand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(
        scanBasePackages = {
                "com.talentgrid.demand",
                "com.talentgrid.kafka",
                "com.talentgrid.audit",
                "com.talentgrid.clients"
        }
)
@EnableFeignClients(basePackages = {"com.talentgrid.demand.client"})
public class DemandServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemandServiceApplication.class, args);
    }
}