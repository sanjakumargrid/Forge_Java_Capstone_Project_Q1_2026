package com.talentgrid.application.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.talentgrid.application.application.dto.RejectionEmailDraftRequestDto;
import com.talentgrid.application.application.dto.RejectionEmailDraftResponseDto;
import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;
import com.talentgrid.application.application.entity.Application;
import com.talentgrid.application.application.repository.ApplicationRepository;
import com.talentgrid.application.client.CandidateClient;
import com.talentgrid.application.integration.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RejectionEmailDraftService {

    private final ApplicationRepository applicationRepository;
    private final CandidateClient candidateClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    public RejectionEmailDraftResponseDto generateDraft(
            Long applicationId,
            RejectionEmailDraftRequestDto request
    ) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found with id: " + applicationId
                ));

        Long candidateId = application.getCandidateId();

        if (candidateId == null) {
            throw new IllegalStateException(
                    "Candidate id is missing for application id: " + applicationId
            );
        }

        ExternalCandidateDto candidate = candidateClient.getCandidateById(candidateId);

        String prompt = """
                Generate a personalised rejection email draft for recruiter review.
                Never auto-send. Return only valid JSON with keys: subject, body.

                Candidate name: %s %s
                Candidate email: %s
                Candidate source: %s
                Current stage: %s
                Rejection reason: %s
                Additional context: %s

                The email must be polite, specific, concise, and professional.
                """
                .formatted(
                        candidate.getFirstName(),
                        candidate.getLastName(),
                        candidate.getEmail(),
                        candidate.getSource(),
                        application.getCurrentStage(),
                        request.getRejectionReason(),
                        request.getAdditionalContext()
                );

        String content = callOpenAi(prompt);
        JsonNode json = parseJson(content);

        return RejectionEmailDraftResponseDto.builder()
                .subject(json.path("subject").asText("Rejection update"))
                .body(json.path("body").asText(content))
                .build();
    }

    private String callOpenAi(String prompt) {

        RestClient restClient = RestClient.builder()
                .baseUrl(openAiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> system = Map.of(
                "role", "system",
                "content", "You are an HR assistant. Return only valid JSON with keys subject and body."
        );

        Map<String, Object> user = Map.of(
                "role", "user",
                "content", prompt
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openAiProperties.getModel());
        payload.put("temperature", 0.4);
        payload.put("messages", List.of(system, user));

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("OpenAI returned an empty response");
        }

        JsonNode choices = response.path("choices");

        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("OpenAI response did not include choices");
        }

        return choices.get(0)
                .path("message")
                .path("content")
                .asText();
    }

    private JsonNode parseJson(String content) {

        String cleaned = content.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("\\s*```$", "");

        try {
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI draft was not valid JSON", e);
        }
    }
}