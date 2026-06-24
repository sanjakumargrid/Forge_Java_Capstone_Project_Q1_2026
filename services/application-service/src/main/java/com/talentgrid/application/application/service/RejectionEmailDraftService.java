package com.talentgrid.application.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.application.application.dto.JobPostingDto;
import com.talentgrid.application.application.dto.RejectionEmailDraftRequestDto;
import com.talentgrid.application.application.dto.RejectionEmailDraftResponseDto;
import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;
import com.talentgrid.application.application.entity.Application;
import com.talentgrid.application.application.repository.ApplicationRepository;
import com.talentgrid.application.client.CandidateClient;
import com.talentgrid.application.client.JobPostingClient;
import com.talentgrid.application.integration.GeminiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RejectionEmailDraftService {

    private final ApplicationRepository applicationRepository;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final CandidateClient candidateClient;
    private final JobPostingClient jobPostingClient;

    public RejectionEmailDraftResponseDto generateDraft(
            Long applicationId,
            RejectionEmailDraftRequestDto request
    ) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found with id: " + applicationId));

        ExternalCandidateDto candidate = null;
        JobPostingDto jobPosting = null;

        try {
            candidate = candidateClient.getCandidateById(application.getCandidateId());
        } catch (Exception e) {
            // Continue with fallback candidate name
        }

        try {
            jobPosting = jobPostingClient.getJobPosting(application.getJobPostingId());
        } catch (Exception e) {
            // Continue with fallback job title
        }

        String candidateName =
                candidate != null
                        ? candidate.getFirstName() + " " + candidate.getLastName()
                        : "Candidate " + application.getCandidateId();

        String jobTitle =
                jobPosting != null
                        ? jobPosting.getTitle()
                        : "Job Posting " + application.getJobPostingId();

        String prompt = """
                Generate a polite personalised rejection email draft.

                Rules:
                - Recruiter will review before sending.
                - Do not say the email was sent.
                - Keep it professional and empathetic.
                - Include subject and body.
                - Return valid JSON only with fields: subject, body.
                - DO NOT USE PLACEHOLDERS like [Candidate Name] or [Job Title]. Use the exact values provided below.

                Candidate Name: %s
                Job Title: %s
                Company Name: TalentGrid
                Current Stage: %s
                Rejection Reason: %s
                Additional Context: %s
                """.formatted(
                candidateName,
                jobTitle,
                application.getCurrentStage(),
                request.getRejectionReason(),
                request.getAdditionalContext()
        );

        String apiKey = geminiProperties.getApiKey();

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("dummy-key")) {
            return RejectionEmailDraftResponseDto.builder()
                    .subject("Update on your application for " + jobTitle)
                    .body("Dear " + candidateName + ",\n\n"
                            + "Thank you for your interest in the " + jobTitle + " role at TalentGrid. "
                            + "After careful review, we will not be moving forward with your application at this time.\n\n"
                            + "We appreciate the time you invested and wish you the best in your job search.\n\n"
                            + "Best regards,\n"
                            + "TalentGrid Recruiting Team\n\n"
                            + "[MOCK EMAIL DRAFT - NO API KEY CONFIGURED]")
                    .build();
        }

        String url = geminiProperties.getBaseUrl()
                + "/models/"
                + geminiProperties.getModel()
                + ":generateContent?key="
                + geminiProperties.getApiKey();

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        String response = RestClient.create()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return parseGeminiResponse(response);
    }

    private RejectionEmailDraftResponseDto parseGeminiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode candidates = root.path("candidates");

            if (candidates.isMissingNode() || !candidates.isArray() || candidates.isEmpty()) {
                throw new IllegalStateException("Gemini response missing candidates array: " + response);
            }

            String text = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            text = text.replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode draftJson = objectMapper.readTree(text);

            return RejectionEmailDraftResponseDto.builder()
                    .subject(draftJson.path("subject").asText())
                    .body(draftJson.path("body").asText())
                    .build();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Gemini rejection email draft response", e);
        }
    }
}