package com.talentgrid.candidate.resumeParser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.candidate.resumeParser.model.ParsedResumeDTO;
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
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class ResumeParserService {

    @Value("${llm.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";


    public ResumeParserService(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public ParsedResumeDTO parseResume(MultipartFile file) throws IOException {

        String resumeText = extractTextFromFile(file);

        String maskedText = maskPII(resumeText);

        String systemPrompt = """
            You are an expert ATS (Applicant Tracking System) parser. 
            Extract information from the provided resume text and return it strictly as a JSON object matching this schema:
            {
              "firstName": "string",
              "lastName": "string",
              "email": "string",
              "phoneNumber": "string",
              "location": "string",
              "summary": "string",
              "websites": ["string"],
              "skills": ["string"],
              "languages": ["string"],
              "workExperience": [
                {
                  "jobTitle": "string",
                  "companyName": "string",
                  "startDate": "string",
                  "endDate": "string",
                  "location": "string",
                  "description": "string"
                }
              ],
              "education": [
                {
                  "institutionName": "string",
                  "degree": "string",
                  "major": "string",
                  "startDate": "string",
                  "endDate": "string",
                  "gpa": "string"
                }
              ]
            }
            If a field is missing in the resume, return null or an empty array for that field. Do not include any markdown formatting.
            """;

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.1,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Here is the resume text:\n\n" + maskedText)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);

        return parseLlmResponseToDTO(response.getBody());
    }

    // BUG FIX: Uses Apache Tika to support PDF, DOCX, TXT, etc., and uses try-with-resources to prevent memory leaks
    private String extractTextFromFile(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            Tika tika = new Tika();
            String extractedText = tika.parseToString(inputStream);
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new IOException("The uploaded document is empty or unreadable.");
            }
            return extractedText;
        } catch (Exception e) {
            throw new IOException("Failed to extract text. Unsupported or corrupted file format: " + e.getMessage(), e);
        }
    }

    private ParsedResumeDTO parseLlmResponseToDTO(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        String jsonContent = rootNode.path("choices").get(0)
                .path("message")
                .path("content").asText();

        return objectMapper.readValue(jsonContent, ParsedResumeDTO.class);
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