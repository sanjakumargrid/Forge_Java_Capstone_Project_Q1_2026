package com.talentgrid.candidate.resumeParser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.candidate.resumeParser.model.AtsEvaluationDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AtsEvaluationDTO evaluateResume(MultipartFile file, String jobDescription) throws IOException {
        // 1. Extract raw text from the resume PDF
        String resumeText = extractTextFromPdf(file);

        // 2. Define the ATS scoring rules for the LLM
        String systemPrompt = """
            You are an expert ATS (Applicant Tracking System) reviewer. 
            Your task is to evaluate the provided resume text against the target job description.
            
            CRITICAL CRITERIA RULES:
            - Focus EXCLUSIVELY on content alignment: relevant skills, experience depth, tool proficiencies, and qualifications.
            - IGNORE visual aspects such as fonts, font sizes, colors, margins, columns, text colors, page limits, or overall visual aesthetics. Do not deduct points for lack of styling.
            
            Provide your final evaluation strictly as a JSON object matching this schema:
            {
              "matchScore": 85, // An integer between 0 and 100 representing job alignment
              "matchedSkills": ["Java", "Spring Boot"], // Key matching skills found
              "missingSkills": ["AWS", "Docker"], // Crucial skills in the JD but absent/weak in the resume
              "recommendations": ["string"], // Actionable suggestions to improve alignment
              "overallFeedback": "string" // A concise summary of the candidate's structural match
            }
            Do not include any markdown syntax wrappers (like ```json). Return only raw JSON.
            """;

        // 3. Target Gemini endpoint
        String geminiUrl = "[https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=](https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=)" + apiKey;

        // 4. Combine prompt, resume text, and job description into the payload
        String userContent = String.format(
                "JOB DESCRIPTION:\n%s\n\n-------------\n\nRESUME TEXT:\n%s",
                jobDescription,
                resumeText
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", systemPrompt + "\n\n" + userContent)
                        ))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        // 5. Build and send request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(geminiUrl, entity, String.class);

        // 6. Map response JSON to the DTO
        return parseLlmResponseToDTO(response.getBody());
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private AtsEvaluationDTO parseLlmResponseToDTO(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        String jsonContent = rootNode.path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text").asText();

        return objectMapper.readValue(jsonContent, AtsEvaluationDTO.class);
    }
}
