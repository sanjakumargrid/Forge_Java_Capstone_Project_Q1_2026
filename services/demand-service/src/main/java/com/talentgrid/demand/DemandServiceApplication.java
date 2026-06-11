package com.talentgrid.demand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
                "com.talentgrid.demand",
                "com.talentgrid.kafka",
                "com.talentgrid.audit"
        }
)
public class DemandServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(
            DemandServiceApplication.class,
            args
    );
  }
}