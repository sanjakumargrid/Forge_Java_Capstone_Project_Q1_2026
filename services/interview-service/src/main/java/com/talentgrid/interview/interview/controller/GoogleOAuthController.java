package com.talentgrid.interview.interview.controller;

import com.talentgrid.interview.config.GoogleOAuthTokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/google/oauth")
@ConditionalOnProperty(name = "google.calendar.auth-mode", havingValue = "oauth")
public class GoogleOAuthController {

    private final GoogleOAuthTokenService googleOAuthTokenService;

    public GoogleOAuthController(GoogleOAuthTokenService googleOAuthTokenService) {
        this.googleOAuthTokenService = googleOAuthTokenService;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize() {
        String url = googleOAuthTokenService.buildAuthorizationUrl();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error
    ) {
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Google OAuth failed with error: " + error);
        }
        if (code == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Missing authorization code. Please restart the authorization flow at /api/v1/google/oauth/authorize");
        }
        
        try {
            googleOAuthTokenService.saveToken(code);
            return ResponseEntity.ok("Google OAuth Authorization Successful! You can now close this tab and resume using the API.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to authorize: " + ex.getMessage());
        }
    }
}
