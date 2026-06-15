package com.talentgrid.candidate.resumeParser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.candidate.resumeParser.model.ParsedResumeDTO;
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
public class ResumeParserService {

    @Value("${llm.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // 1. Declare the field without instantiating it
    private final ObjectMapper objectMapper;

    // 2. Let Spring Boot automatically inject its pre-configured ObjectMapper
    public ResumeParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    // Groq's API endpoint (which is OpenAI compatible)
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public ParsedResumeDTO parseResume(MultipartFile file) throws IOException {
        // 1. Extract text from PDF
        String resumeText = extractTextFromFile(file);

        // 2. Prepare the ATS parsing prompt
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

        // 3. Groq (OpenAI-compatible) Payload structure
        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile", // Or use "llama-3.1-8b-instant"
                "response_format", Map.of("type", "json_object"), // Enforces strict JSON output
                "temperature", 0.1, // Low temperature for high accuracy data extraction
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Here is the resume text:\n\n" + resumeText)
                )
        );

        // 4. Set Headers (Groq requires Bearer Auth)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 5. Call the Groq API
        ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, entity, String.class);

        // 6. Parse the LLM response into our DTO
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

    private ParsedResumeDTO parseLlmResponseToDTO(String responseBody) throws IOException {
        // Navigate the standard OpenAI/Groq JSON structure
        JsonNode rootNode = objectMapper.readTree(responseBody);
        String jsonContent = rootNode.path("choices").get(0)
                .path("message")
                .path("content").asText();

        // Deserialize the raw JSON string directly into the Java Record
        return objectMapper.readValue(jsonContent, ParsedResumeDTO.class);
    }
}