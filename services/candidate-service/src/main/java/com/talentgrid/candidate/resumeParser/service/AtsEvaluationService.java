package com.talentgrid.candidate.resumeParser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.candidate.resumeParser.model.AtsEvaluationDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class AtsEvaluationService {

    @Value("${llm.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    // Groq's API endpoint
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // NO parameters in this constructor!
    public AtsEvaluationService() {
        this.objectMapper = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Now accepts the List of skills directly from your Demand Service
    public AtsEvaluationDTO evaluateResume(MultipartFile file, List<String> demandSkills) throws IOException {
        // 1. Extract raw text from the resume PDF
        String resumeText = extractTextFromFile(file);

        // Convert the list of skills into a comma-separated string for the AI to read easily
        String requiredSkillsText = String.join(", ", demandSkills);

        // 2. Define the ATS scoring rules for the LLM
        String systemPrompt = """
            You are an expert ATS (Applicant Tracking System) reviewer. 
            Your task is to evaluate the provided resume text against the target required skills.
            
            CRITICAL CRITERIA RULES:
            - Focus EXCLUSIVELY on content alignment: Check if the required skills are present in the resume.
            - IGNORE visual aspects such as fonts, font sizes, colors, margins, columns, text colors, page limits, or overall visual aesthetics. Do not deduct points for lack of styling.
            
            Provide your final evaluation strictly as a JSON object matching this schema:
            {
              "matchScore": 85, // An integer between 0 and 100 based on how many required skills are met
              "matchedSkills": ["string"], // Required skills that ARE present in the resume
              "unmatchedSkills": ["string"], // Required skills that are MISSING from the resume
              "recommendations": ["string"], // Actionable suggestions to improve alignment
              "overallFeedback": "string" // A concise summary of the candidate's match
            }
            Do not include any markdown syntax wrappers (like ```json). Return only raw JSON.
            """;

        // 3. Groq (OpenAI-compatible) Payload structure
        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "response_format", Map.of("type", "json_object"), // Enforces strict JSON output
                "temperature", 0.1,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "REQUIRED SKILLS:\n" + requiredSkillsText + "\n\n-------------\n\nRESUME TEXT:\n" + resumeText)
                )
        );

        // 4. Set Headers (Groq requires Bearer Auth)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 5. Call the Groq API
        ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, entity, String.class);

        // 6. Map response JSON to the DTO
        return parseLlmResponseToDTO(response.getBody());
    }

    private String extractTextFromFile(MultipartFile file) throws IOException {
        try {
            Tika tika = new Tika();
            // Tika automatically detects if it's a PDF, DOCX, TXT, etc., and pulls the text
            String extractedText = tika.parseToString(file.getInputStream());

            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new IOException("The extracted document is empty or unreadable.");
            }
            return extractedText;

        } catch (Exception e) {
            throw new IOException("Failed to parse document. Unsupported or corrupted file.", e);
        }
    }

    private AtsEvaluationDTO parseLlmResponseToDTO(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        String jsonContent = rootNode.path("choices").get(0)
                .path("message")
                .path("content").asText();

        return objectMapper.readValue(jsonContent, AtsEvaluationDTO.class);
    }
}