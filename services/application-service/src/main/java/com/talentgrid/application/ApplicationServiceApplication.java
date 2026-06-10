package com.talentgrid.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
                "com.talentgrid.application",
                "com.talentgrid.kafka",
                "com.talentgrid.audit"
        }
)
public class ApplicationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(
            ApplicationServiceApplication.class,
            args
    );
  }
}