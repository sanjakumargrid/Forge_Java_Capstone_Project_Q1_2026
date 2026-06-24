package com.talentgrid.workforce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.talentgrid")
@EnableFeignClients(basePackages = "com.talentgrid.workforce")
@EnableKafka
@EnableScheduling
public class WorkforceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkforceServiceApplication.class, args);
        System.out.println("testing main method");
    }
}