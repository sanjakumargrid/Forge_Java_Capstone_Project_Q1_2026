package com.talentgrid.candidate.resumeParser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.candidate.resumeParser.model.ParsedResumeDTO;
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
public class ResumeParserService {

    @Value("${llm.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedResumeDTO parseResume(MultipartFile file) throws IOException {
        // 1. Extract text from PDF
        String resumeText = extractTextFromPdf(file);

        // 2. Prepare the prompt instructing the LLM to output our exact JSON structure
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
            If a field is missing in the resume, return null or an empty array for that field. Do not include any markdown formatting, just the raw JSON object.
            """;

        // 3. Dynamic URL with API Key for Gemini
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        // 4. Gemini-specific Payload
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                // Gemini doesn't separate System and User prompts as strictly in basic REST calls,
                                // so we combine them into one clear instruction block.
                                Map.of("text", systemPrompt + "\n\nHere is the resume:\n" + resumeText)
                        ))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json" // Forces JSON output
                )
        );

        // 5. Headers (No Bearer token needed here, just Content-Type)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 6. Call the Gemini API
        ResponseEntity<String> response = restTemplate.postForEntity(geminiUrl, entity, String.class);

        // 7. Parse the LLM response into our DTO
        return parseLlmResponseToDTO(response.getBody());
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private ParsedResumeDTO parseLlmResponseToDTO(String responseBody) throws IOException {
        // Navigate the Gemini API response JSON
        JsonNode rootNode = objectMapper.readTree(responseBody);
        String jsonContent = rootNode.path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text").asText();

        // Deserialize the actual JSON string from the LLM directly into our Java Record
        return objectMapper.readValue(jsonContent, ParsedResumeDTO.class);
    }
}





















//gpt 4.0
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.talentgrid.candidate.resumeParser.model.ParsedResumeDTO;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.text.PDFTextStripper;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class ResumeParserService {
//
//    @Value("${llm.api.key}")
//    private String apiKey;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    // Change this URL if using Grok or Gemini
//    private static final String LLM_API_URL = "https://api.openai.com/v1/chat/completions";
//
//    public ParsedResumeDTO parseResume(MultipartFile file) throws IOException {
//        // 1. Extract text from PDF
//        String resumeText = extractTextFromPdf(file);
//
//        // 2. Prepare the prompt instructing the LLM to output our exact JSON structure
//        String systemPrompt = """
//            You are an expert ATS (Applicant Tracking System) parser.
//            Extract information from the provided resume text and return it strictly as a JSON object matching this schema:
//            {
//              "firstName": "string",
//              "lastName": "string",
//              "email": "string",
//              "phoneNumber": "string",
//              "location": "string",
//              "summary": "string",
//              "websites": ["string"],
//              "skills": ["string"],
//              "languages": ["string"],
//              "workExperience": [
//                {
//                  "jobTitle": "string",
//                  "companyName": "string",
//                  "startDate": "string",
//                  "endDate": "string",
//                  "location": "string",
//                  "description": "string"
//                }
//              ],
//              "education": [
//                {
//                  "institutionName": "string",
//                  "degree": "string",
//                  "major": "string",
//                  "startDate": "string",
//                  "endDate": "string",
//                  "gpa": "string"
//                }
//              ]
//            }
//            If a field is missing in the resume, return null or an empty array for that field. Do not include any markdown formatting, just the raw JSON object.
//            """;
//
//        // 3. Create the payload for the LLM API
//        Map<String, Object> requestBody = Map.of(
//                "model", "gpt-4o-mini", // Cost-effective and great at JSON
//                "response_format", Map.of("type", "json_object"), // Forces JSON output
//                "messages", List.of(
//                        Map.of("role", "system", "content", systemPrompt),
//                        Map.of("role", "user", "content", "Here is the resume text:\n\n" + resumeText)
//                )
//        );
//
//        // 4. Set Headers
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(apiKey);
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        // 5. Call the LLM API
//        ResponseEntity<String> response = restTemplate.postForEntity(LLM_API_URL, entity, String.class);
//
//
//        return parseLlmResponseToDTO(response.getBody());
//    }
//
//    private String extractTextFromPdf(MultipartFile file) throws IOException {
//        try (PDDocument document = PDDocument.load(file.getInputStream())) {
//            PDFTextStripper stripper = new PDFTextStripper();
//            return stripper.getText(document);
//        }
//    }
//
//    private ParsedResumeDTO parseLlmResponseToDTO(String responseBody) throws IOException {
//        // Navigate the OpenAI API response JSON: choices[0].message.content
//        JsonNode rootNode = objectMapper.readTree(responseBody);
//        String jsonContent = rootNode.path("choices").get(0).path("message").path("content").asText();
//
//        // Deserialize the actual JSON string from the LLM directly into our Java Record
//        return objectMapper.readValue(jsonContent, ParsedResumeDTO.class);
//    }
//}

