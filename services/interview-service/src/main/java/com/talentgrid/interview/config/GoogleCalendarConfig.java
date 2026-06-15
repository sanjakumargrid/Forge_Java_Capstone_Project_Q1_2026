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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Slf4j
@Configuration
public class GoogleCalendarConfig {

    /**
     * Absolute path to your Google Service Account JSON key file.
     * Set via environment variable: GOOGLE_SERVICE_ACCOUNT_KEY_PATH
     * e.g. /etc/secrets/talentgrid-service-account.json
     */
    @Value("${google.calendar.service-account-key-path}")
    private String serviceAccountKeyPath;

    /**
     * The Google Workspace domain admin email used for domain-wide delegation.
     * The service account impersonates this account to access interviewer calendars.
     * Set via environment variable: GOOGLE_CALENDAR_DELEGATE_EMAIL
     */
    @Value("${google.calendar.delegate-email}")
    private String delegateEmail;

    /**
     * Your application name registered in Google Cloud Console.
     */
    @Value("${google.calendar.application-name:TalentGrid}")
    private String applicationName;

    /**
     * Builds and returns a Google Calendar API service client authenticated
     * via a Service Account with domain-wide delegation.
     *
     * Setup required in Google Cloud Console:
     * 1. Create a Service Account in your Google Cloud project.
     * 2. Enable domain-wide delegation on the service account.
     * 3. In Google Workspace Admin → Security → API Controls → Domain-wide Delegation,
     *    add the service account client ID with scope:
     *    https://www.googleapis.com/auth/calendar
     * 4. Download the service account JSON key and set its path in application.properties.
     */
    @Bean
    public Calendar googleCalendarService() throws GeneralSecurityException, IOException {
        log.info("[GoogleCalendarConfig] Initialising Google Calendar API service with service account: {}",
                serviceAccountKeyPath);

        GoogleCredentials credentials = ServiceAccountCredentials
                .fromStream(new FileInputStream(serviceAccountKeyPath))
                .createScoped(List.of(CalendarScopes.CALENDAR))
                .createDelegated(delegateEmail);

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        )
                .setApplicationName(applicationName)
                .build();
    }
}
