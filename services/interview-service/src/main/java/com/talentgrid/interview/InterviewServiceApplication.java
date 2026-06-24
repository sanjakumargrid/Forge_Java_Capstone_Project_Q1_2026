package com.talentgrid.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.talentgrid"})
public class InterviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewServiceApplication.class, args);
    }
}