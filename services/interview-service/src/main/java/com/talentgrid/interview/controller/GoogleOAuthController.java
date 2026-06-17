package com.talentgrid.interview.controller;

import com.talentgrid.interview.config.GoogleOAuthTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "google.calendar.auth-mode", havingValue = "oauth")
public class GoogleOAuthController {

    private final GoogleOAuthTokenService googleOAuthTokenService;

    @GetMapping("/api/google/oauth/authorize")
    public ResponseEntity<Void> authorize() {
        String authorizationUrl = googleOAuthTokenService.buildAuthorizationUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(authorizationUrl));

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/api/google/oauth/callback")
    public ResponseEntity<Map<String, String>> callback(@RequestParam("code") String code) {
        googleOAuthTokenService.saveToken(code);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Google OAuth authorization completed. You can now create real Google Meet links."
        ));
    }

    @GetMapping("/api/google/oauth/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "authorized", googleOAuthTokenService.isAuthorized()
        ));
    }
}