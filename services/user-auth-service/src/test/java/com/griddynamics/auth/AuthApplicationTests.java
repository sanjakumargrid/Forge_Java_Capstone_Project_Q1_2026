package com.talentgrid.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		properties = {
				"jwt.secret=test-jwt-secret-must-be-at-least-256-bits-for-hs256-please-use-long-key",
				"jwt.expiration=604800000",
				"jwt.refresh-expiration=604800000",
				"jwt.issuer=auth-service-test",
				"spring.security.oauth2.client.registration.google.client-id=test-google-client-id",
				"spring.security.oauth2.client.registration.google.client-secret=test-google-client-secret",
				"spring.datasource.url=jdbc:h2:mem:auth_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
				"spring.datasource.username=sa",
				"spring.datasource.password=",
				"spring.datasource.driver-class-name=org.h2.Driver",
				"spring.jpa.hibernate.ddl-auto=create-drop",
				"spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"
		})
class AuthApplicationTests {

	@Test
	void contextLoads() {
	}

}
