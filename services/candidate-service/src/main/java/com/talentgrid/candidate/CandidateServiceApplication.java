package com.talentgrid.candidate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.talentgrid.candidate",
        "com.talentgrid.audit.client",
        "com.talentgrid.kafka.producer"
})
public class CandidateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CandidateServiceApplication.class, args);
    }
}