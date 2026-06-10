package com.talentgrid.candidate.resumeParser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.talentgrid.candidate.resumeParser.model.ParsedResumeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeParseService {

    private final String AFFINDA_URL = "https://api.affinda.com/v3/documents";

    @Value("${affinda.api.key}")
    private String apiKey;

    @Value("${affinda.workspace.id}")
    private String workspaceId;

    // Inject the new mapping service here!
    @Autowired
    private ResumeMappingService mappingService;

    public ParsedResumeDTO parseResume(MultipartFile resumeFile) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();

            // 1. Prepare Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + apiKey);

            // 2. Prepare Body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(resumeFile.getBytes()) {
                @Override
                public String getFilename() {
                    return resumeFile.getOriginalFilename();
                }
            };

            body.add("file", fileResource);
            body.add("workspace", workspaceId);
            body.add("wait", "true");
            //body.add("documentType", "resume");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 3. Call Affinda API
            ResponseEntity<String> response = restTemplate.exchange(
                    AFFINDA_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // 4. Delegate parsing to the Mapping Service!
            JsonNode rootNode = mapper.readTree(response.getBody());
            String rawJson = response.getBody();
            System.out.println("RAW AFFINDA JSON: " + rawJson);
            return mappingService.mapToDTO(rootNode);

        } catch (Exception e) {
            System.err.println("Failed to parse resume: " + e.getMessage());
            return null;
        }
    }
}














//@Service
//public class ResumeParseService {
//
//    private final String AFFINDA_URL = "https://api.affinda.com/v3/documents";
//
//    @Value("${affinda.api.key}")
//    private String apiKey;
//
//    @Value("${affinda.workspace.id}")
//    private String workspaceId;
//
//    public ParsedResumeDTO parseResume(MultipartFile resumeFile) {
//        try {
//            RestTemplate restTemplate = new RestTemplate();
//            ObjectMapper mapper = new ObjectMapper();
//
//            // 1. Prepare Headers
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//            headers.set("Authorization", "Bearer " + apiKey);
//
//            // 2. Prepare Body
//            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//            ByteArrayResource fileResource = new ByteArrayResource(resumeFile.getBytes()) {
//                @Override
//                public String getFilename() {
//                    return resumeFile.getOriginalFilename();
//                }
//            };
//
//            body.add("file", fileResource);
//            body.add("workspace", workspaceId);
//            body.add("wait", "true");
//
//            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//
//            // 3. Call Affinda API
//            ResponseEntity<String> response = restTemplate.exchange(
//                    AFFINDA_URL,
//                    HttpMethod.POST,
//                    requestEntity,
//                    String.class
//            );
//
//            // 4. Parse response and map to your Records
//            JsonNode rootNode = mapper.readTree(response.getBody());
//            return mapToDTO(rootNode);
//
//        } catch (Exception e) {
//            System.err.println("Failed to parse resume: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private ParsedResumeDTO mapToDTO(JsonNode rootNode) {
//        JsonNode data = rootNode.path("data");
//
//        // Basic Info
//        String firstName = data.path("name").path("first").asText(null);
//        String lastName = data.path("name").path("last").asText(null);
//        String email = data.path("emails").isArray() && !data.path("emails").isEmpty()
//                ? data.path("emails").get(0).asText(null) : null;
//        String phoneNumber = data.path("phoneNumbers").isArray() && !data.path("phoneNumbers").isEmpty()
//                ? data.path("phoneNumbers").get(0).asText(null) : null;
//        String location = data.path("location").path("formatted").asText(null);
//        String summary = data.path("summary").asText(null);
//
//        // Simple Lists
//        List<String> websites = extractList(data.path("websites"), "url");
//        List<String> skills = extractList(data.path("skills"), "name");
//        List<String> languages = extractList(data.path("languages"), "language");
//
//        // Work Experience List
//        List<WorkExperienceDTO> workList = new ArrayList<>();
//        if (data.path("workExperience").isArray()) {
//            for (JsonNode job : data.path("workExperience")) {
//                workList.add(new WorkExperienceDTO(
//                        job.path("jobTitle").asText(null),
//                        job.path("organization").asText(null),
//                        job.path("dates").path("startDate").asText(null),
//                        job.path("dates").path("endDate").asText(null),
//                        job.path("location").path("formatted").asText(null),
//                        job.path("jobDescription").asText(null)
//                ));
//            }
//        }
//
//        // Education List
//        List<EducationDTO> eduList = new ArrayList<>();
//        if (data.path("education").isArray()) {
//            for (JsonNode edu : data.path("education")) {
//                eduList.add(new EducationDTO(
//                        edu.path("organization").asText(null),
//                        edu.path("accreditation").path("educationLevel").asText(null),
//                        edu.path("accreditation").path("education").asText(null),
//                        edu.path("dates").path("startDate").asText(null),
//                        edu.path("dates").path("completionDate").asText(null),
//                        edu.path("grade").path("value").asText(null)
//                ));
//            }
//        }
//
//        // Combine into main DTO
//        return new ParsedResumeDTO(
//                firstName, lastName, email, phoneNumber, location, summary,
//                websites, skills, languages, workList, eduList
//        );
//    }
//
//    // Helper to safely extract flat lists from Affinda's nested arrays
//    private List<String> extractList(JsonNode node, String fieldName) {
//        List<String> list = new ArrayList<>();
//        if (node.isArray()) {
//            for (JsonNode item : node) {
//                String val = item.has(fieldName) ? item.path(fieldName).asText() : item.asText();
//                if (val != null && !val.isBlank()) {
//                    list.add(val);
//                }
//            }
//        }
//        return list;
//    }
//}