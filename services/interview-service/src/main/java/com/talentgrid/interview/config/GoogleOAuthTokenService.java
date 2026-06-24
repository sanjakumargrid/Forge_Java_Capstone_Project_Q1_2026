package com.talentgrid.interview.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "google.calendar.auth-mode", havingValue = "oauth")
public class GoogleOAuthTokenService {

    private static final String USER_ID = "talentgrid-google-user";

    private final ResourceLoader resourceLoader;

    @Value("${google.oauth.client-secret-path}")
    private String clientSecretPath;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${google.oauth.token-store-dir:google-oauth-tokens}")
    private String tokenStoreDir;

    @Value("${google.calendar.application-name:TalentGrid}")
    private String applicationName;

    public GoogleOAuthTokenService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String buildAuthorizationUrl() {
        try {
            GoogleAuthorizationCodeFlow flow = createFlow();

            return flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .setAccessType("offline")
                    .set("prompt", "consent")
                    .build();

        } catch (Exception ex) {
            log.error("[GoogleOAuthTokenService] Failed to build Google OAuth URL", ex);
            throw new IllegalStateException("Failed to build Google OAuth URL: " + ex.getMessage(), ex);
        }
    }

    public void saveToken(String code) {
        try {
            GoogleAuthorizationCodeFlow flow = createFlow();

            GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(redirectUri)
                    .execute();

            flow.createAndStoreCredential(tokenResponse, USER_ID);

            log.info("[GoogleOAuthTokenService] Google OAuth token saved successfully");

        } catch (Exception ex) {
            log.error("[GoogleOAuthTokenService] Failed to save Google OAuth token", ex);
            throw new IllegalStateException("Failed to save Google OAuth token: " + ex.getMessage(), ex);
        }
    }

    public boolean isAuthorized() {
        try {
            Credential credential = createFlow().loadCredential(USER_ID);
            return credential != null
                    && (credential.getRefreshToken() != null || credential.getAccessToken() != null);
        } catch (Exception ex) {
            log.error("[GoogleOAuthTokenService] isAuthorized failed: ", ex);
            return false;
        }
    }

    public Calendar getCalendarService() {
        try {
            Credential credential = createFlow().loadCredential(USER_ID);

            if (credential == null) {
                throw new IllegalStateException(
                        "Google OAuth not authorized. Open /api/v1/google/oauth/authorize first."
                );
            }

            if (credential.getExpiresInSeconds() != null
                    && credential.getExpiresInSeconds() <= 60
                    && credential.getRefreshToken() != null) {
                credential.refreshToken();
            }

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
            )
                    .setApplicationName(applicationName)
                    .build();

        } catch (Exception ex) {
            log.error("[GoogleOAuthTokenService] Failed to create OAuth Calendar service", ex);
            throw new IllegalStateException("Failed to create OAuth Calendar service: " + ex.getMessage(), ex);
        }
    }

    private GoogleAuthorizationCodeFlow createFlow() throws IOException, GeneralSecurityException {

        Resource resource = resourceLoader.getResource(clientSecretPath);

        if (!resource.exists()) {
            throw new IOException("Google OAuth client secret file not found: " + clientSecretPath);
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(resource.getInputStream())
        );

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                List.of(CalendarScopes.CALENDAR)
        )
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokenStoreDir)))
                .setAccessType("offline")
                .build();
    }
}