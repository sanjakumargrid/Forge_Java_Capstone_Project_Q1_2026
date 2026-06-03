package com.talentgrid.demand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DemandServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemandServiceApplication.class, args);
    }
}
