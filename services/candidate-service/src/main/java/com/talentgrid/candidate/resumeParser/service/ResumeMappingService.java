package com.talentgrid.candidate.resumeParser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.talentgrid.candidate.resumeParser.model.EducationDTO;
import com.talentgrid.candidate.resumeParser.model.ParsedResumeDTO;
import com.talentgrid.candidate.resumeParser.model.WorkExperienceDTO;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;


@Service
public class ResumeMappingService {

    public ParsedResumeDTO mapToDTO(JsonNode rootNode) {
        JsonNode data = rootNode.path("data");

        // Basic Info (Updated to match your raw JSON structure)
        String firstName = getSafeString(data.path("firstName"));
        String lastName = getSafeString(data.path("lastName"));
        String email = getSafeString(data.path("email"));
        String phoneNumber = getSafeString(data.path("phoneNumber"));
        String location = getSafeString(data.path("location"));
        String summary = getSafeString(data.path("summary"));

        // Simple Lists
        List<String> websites = extractList(data.path("websites"));
        List<String> skills = extractList(data.path("skills"));
        List<String> languages = extractList(data.path("languages"));

        // Work Experience List
        List<WorkExperienceDTO> workList = new ArrayList<>();
        if (data.path("workExperience").isArray()) {
            for (JsonNode jobContainer : data.path("workExperience")) {
                // In your JSON, the actual job data is nested inside a "parsed" object
                JsonNode job = jobContainer.path("parsed");

                workList.add(new WorkExperienceDTO(
                        getSafeString(job.path("jobTitle")),
                        getSafeString(job.path("companyName")),
                        getSafeString(job.path("startDate")),
                        getSafeString(job.path("endDate")),
                        getSafeString(job.path("location")),
                        getSafeString(job.path("description"))
                ));
            }
        }

        // Education List
        List<EducationDTO> eduList = new ArrayList<>();
        if (data.path("education").isArray()) {
            for (JsonNode eduContainer : data.path("education")) {
                // In your JSON, the actual education data is nested inside a "parsed" object
                JsonNode edu = eduContainer.path("parsed");

                eduList.add(new EducationDTO(
                        getSafeString(edu.path("institutionName")),
                        getSafeString(edu.path("degree")),
                        getSafeString(edu.path("major")),
                        getSafeString(edu.path("startDate")),
                        getSafeString(edu.path("endDate")),
                        getSafeString(edu.path("gpa"))
                ));
            }
        }

        return new ParsedResumeDTO(
                firstName, lastName, email, phoneNumber, location, summary,
                websites, skills, languages, workList, eduList
        );
    }

    private List<String> extractList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String val = getSafeString(item);
                if (val != null && !val.isBlank()) {
                    list.add(val);
                }
            }
        }
        return list;
    }

    private String getSafeString(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isObject()) {
            // Affinda's custom models usually put the clean text in "parsed" or "raw"
            if (node.has("parsed") && !node.path("parsed").isNull()) {
                if(node.path("parsed").isTextual()) return node.path("parsed").asText();
            }
            if (node.has("raw") && !node.path("raw").isNull()) {
                if(node.path("raw").isTextual()) return node.path("raw").asText();
            }
        }
        return null;
    }
}










//@Service
//public class ResumeMappingService {
//
//    public ParsedResumeDTO mapToDTO(JsonNode affindaData) {
//        JsonNode dataNode = affindaData.path("data");
//
//
//        String firstName = dataNode.path("name").path("first").asText(null);
//        String lastName = dataNode.path("name").path("last").asText(null);
//
//        String email = dataNode.path("emails").isArray() && dataNode.path("emails").size() > 0
//                ? dataNode.path("emails").get(0).asText(null) : null;
//
//        String phone = dataNode.path("phoneNumbers").isArray() && dataNode.path("phoneNumbers").size() > 0
//                ? dataNode.path("phoneNumbers").get(0).asText(null) : null;
//
//        String location = dataNode.path("location").path("formatted").asText(null);
//        String summary = dataNode.path("summary").asText(null);
//
//
//        List<String> skills = extractStringList(dataNode.path("skills"), "name");
//        List<String> languages = extractStringList(dataNode.path("languages"), "language");
//        List<String> websites = extractStringList(dataNode.path("websites"), "url"); // Or whatever field Affinda uses
//
//
//        List<WorkExperienceDTO> workList = new ArrayList<>();
//        if (dataNode.path("workExperience").isArray()) {
//            for (JsonNode job : dataNode.path("workExperience")) {
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
//
//        List<EducationDTO> eduList = new ArrayList<>();
//        if (dataNode.path("education").isArray()) {
//            for (JsonNode edu : dataNode.path("education")) {
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
//
//        return new ParsedResumeDTO(
//                firstName, lastName, email, phone, location, summary,
//                websites, skills, languages, workList, eduList
//        );
//    }
//
//
//    private List<String> extractStringList(JsonNode node, String fieldName) {
//        List<String> list = new ArrayList<>();
//        if (node.isArray()) {
//            for (JsonNode item : node) {
//                list.add(item.path(fieldName).asText());
//            }
//        }
//        return list;
//    }
//}