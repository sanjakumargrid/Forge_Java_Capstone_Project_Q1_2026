package com.talentgrid.interview.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

@Service
public class GoogleOAuthTokenService {

    private static final String USER_ID = "talentgrid-user";

    private final ResourceLoader resourceLoader;

    @Value("${google.oauth.client-secret-path}")
    private String clientSecretPath;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${google.oauth.token-store-dir:google-oauth-tokens}")
    private String tokenStoreDir;

    public GoogleOAuthTokenService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String buildAuthorizationUrl() {
        try {
            GoogleAuthorizationCodeFlow flow = buildFlow();

            return flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .setAccessType("offline")
                    .set("prompt", "consent")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to build Google OAuth authorization URL: " + e.getMessage(),
                    e
            );
        }
    }

    public void saveToken(String code) {
        try {
            GoogleAuthorizationCodeFlow flow = buildFlow();

            TokenResponse tokenResponse =
                    flow.newTokenRequest(code)
                            .setRedirectUri(redirectUri)
                            .execute();

            flow.createAndStoreCredential(tokenResponse, USER_ID);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to save Google OAuth token: " + e.getMessage(),
                    e
            );
        }
    }

    public Credential getCredential() {
        try {
            GoogleAuthorizationCodeFlow flow = buildFlow();

            Credential credential = flow.loadCredential(USER_ID);

            if (credential == null) {
                throw new RuntimeException(
                        "Google OAuth not authorized. Open http://localhost:8087/api/v1/google/oauth/authorize first."
                );
            }

            if (credential.getExpiresInSeconds() != null
                    && credential.getExpiresInSeconds() <= 60) {
                credential.refreshToken();
            }

            return credential;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load Google OAuth credential: " + e.getMessage(),
                    e
            );
        }
    }

    private GoogleAuthorizationCodeFlow buildFlow() throws Exception {

        Resource resource = resourceLoader.getResource(clientSecretPath);

        if (!resource.exists()) {
            throw new RuntimeException(
                    "Google OAuth client secret file not found at: " + clientSecretPath
            );
        }

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(
                        GsonFactory.getDefaultInstance(),
                        new InputStreamReader(resource.getInputStream())
                );

        File tokenDirectory = new File(tokenStoreDir);

        if (!tokenDirectory.exists()) {
            tokenDirectory.mkdirs();
        }

        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                List.of(CalendarScopes.CALENDAR_EVENTS)
        )
                .setDataStoreFactory(new FileDataStoreFactory(tokenDirectory))
                .setAccessType("offline")
                .build();
    }
}