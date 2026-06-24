package com.talentgrid.candidate.resumeParser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.candidate.exception.BusinessException;
import com.talentgrid.candidate.resumeParser.model.AtsEvaluationDTO;
import com.talentgrid.candidate.resumeParser.model.DemandDTO;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class AtsEvaluationService {

    @Value("${llm.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";


    public AtsEvaluationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public AtsEvaluationDTO evaluateResume(MultipartFile file, DemandDTO demand) {
        String resumeText = extractTextFromFile(file);

        String maskedText = maskPII(resumeText);

        String demandContext = String.format(
                "ROLE: %s\nREQUIRED EXPERIENCE: %d years\nPRIMARY SKILLS (Critical): %s\nADD-ON SKILLS (Bonus): %s\nJOB DESCRIPTION: %s",
                demand.getTitle(),
                demand.getYearsOfExperience() != null ? demand.getYearsOfExperience() : 0,
                demand.getPrimarySkills() != null ? String.join(", ", demand.getPrimarySkills()) : "None specified",
                demand.getAddOnSkills() != null ? String.join(", ", demand.getAddOnSkills()) : "None specified",
                demand.getDescription()
        );

        String systemPrompt = """
            You are an expert ATS (Applicant Tracking System) reviewer. 
            Evaluate the provided RESUME TEXT against the provided JOB DEMAND.
            
            CRITICAL CRITERIA RULES:
            - Experience Threshold: Calculate total relevant years of experience based on chronological dates in the work history. Drastically lower the aiScore if experience is below REQUIRED EXPERIENCE.
            - Weighted Scoring: Base the `aiScore` heavily on the presence and application of PRIMARY SKILLS. Missing primary skills must drastically lower the score. ADD-ON SKILLS act as a bonus but cannot replace missing primary skills.
            - Strict Text Matching: Identify which requested skills appear explicitly in the RESUME TEXT.
            - Practical Evidence: Verify if matched skills are actively utilized in described projects or roles. Do not infer unstated experience.
            - Extra Capabilities: Identify valuable technical skills present in the resume that were NOT requested in the demand.
            
            Provide your final evaluation strictly as a JSON object matching this exact schema:
            {
              "aiScore": 0,
              "matchedSkills": [],
              "missingSkills": [],
              "otherSkills": [],
              "recommendations": [],
              "overallFeedback": ""
            }
            
            Return ONLY raw JSON. No markdown wrappers.
            """;

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.1,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "JOB DEMAND:\n" + demandContext + "\n\n-------------\n\nRESUME TEXT:\n" + maskedText)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);
            return parseLlmResponseToDTO(response.getBody());
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI evaluation service is currently unreachable: " + e.getMessage());
        }
    }


    private String extractTextFromFile(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            Tika tika = new Tika();
            String extractedText = tika.parseToString(inputStream);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "The uploaded document is empty or unreadable.");
            }
            return extractedText;
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Failed to extract text. Unsupported or corrupted file format.");
        }
    }

    private AtsEvaluationDTO parseLlmResponseToDTO(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choices = rootNode.path("choices");
            if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "LLM response missing 'choices' array (possible rate limit): " + responseBody);
            }
            String jsonContent = choices.get(0)
                    .path("message")
                    .path("content").asText();

            return objectMapper.readValue(jsonContent, AtsEvaluationDTO.class);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse AI output into valid format.");
        }
    }

    private String maskPII(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String emailRegex = "([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})";
        text = text.replaceAll(emailRegex, "[EMAIL REDACTED]");

        String phoneRegex = "(\\+\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}";
        text = text.replaceAll(phoneRegex, "[PHONE REDACTED]");

        String linkedInRegex = "(https?://)?(www\\.)?(linkedin\\.com/in/[a-zA-Z0-9_-]+)";
        text = text.replaceAll(linkedInRegex, "[LINKEDIN REDACTED]");

        String generalUrlRegex = "(https?://\\S+)";
        text = text.replaceAll(generalUrlRegex, "https://www.merriam-webster.com/dictionary/redacted");

        return text;
    }
}