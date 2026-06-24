package com.talentgrid.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.talentgrid"})
public class UserAuthServiceApplication {

	public static void main(String[] args) {
		 SpringApplication.run(UserAuthServiceApplication.class, args);
	}

}
