package com.talentgrid.interview.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(
        name = "google.calendar.auth-mode",
        havingValue = "service-account",
        matchIfMissing = true
)
public class GoogleCalendarConfig {

    private final ResourceLoader resourceLoader;

    public GoogleCalendarConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Value("${google.calendar.service-account-key-path}")
    private String serviceAccountKeyPath;

    @Value("${google.calendar.delegate-email:}")
    private String delegateEmail;

    @Value("${google.calendar.application-name:TalentGrid}")
    private String applicationName;

    @Bean
    @ConditionalOnProperty(
            name = "google.calendar.enabled",
            havingValue = "true"
    )
    public Calendar googleCalendarService() throws GeneralSecurityException, IOException {

        log.info("[GoogleCalendarConfig] Initialising Google Calendar API service with service account path: {}",
                serviceAccountKeyPath);

        Resource resource = resourceLoader.getResource(serviceAccountKeyPath);

        if (!resource.exists()) {
            throw new IOException("Google service account file not found at path: " + serviceAccountKeyPath);
        }

        try (InputStream inputStream = resource.getInputStream()) {

            GoogleCredentials credentials = ServiceAccountCredentials
                    .fromStream(inputStream)
                    .createScoped(List.of(CalendarScopes.CALENDAR));

            if (StringUtils.hasText(delegateEmail)) {
                log.info("[GoogleCalendarConfig] Domain-wide delegation enabled. Delegate email: {}",
                        delegateEmail);

                credentials = credentials.createDelegated(delegateEmail);
            } else {
                log.info("[GoogleCalendarConfig] No delegate email configured. Using service account directly.");
            }

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName(applicationName)
                    .build();
        }
    }
}