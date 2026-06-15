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

        String firstName = getSafeString(data.path("firstName"));
        String lastName = getSafeString(data.path("lastName"));
        String email = getSafeString(data.path("email"));
        String phoneNumber = getSafeString(data.path("phoneNumber"));
        String location = getSafeString(data.path("location"));
        String summary = getSafeString(data.path("summary"));

        List<String> websites = extractList(data.path("websites"));
        List<String> skills = extractList(data.path("skills"));
        List<String> languages = extractList(data.path("languages"));

        List<WorkExperienceDTO> workList = new ArrayList<>();
        if (data.path("workExperience").isArray()) {
            for (JsonNode jobContainer : data.path("workExperience")) {
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

        List<EducationDTO> eduList = new ArrayList<>();
        if (data.path("education").isArray()) {
            for (JsonNode eduContainer : data.path("education")) {
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
            if (node.has("parsed") && !node.path("parsed").isNull()) {
                if (node.path("parsed").isTextual()) return node.path("parsed").asText();
            }
            if (node.has("raw") && !node.path("raw").isNull()) {
                if (node.path("raw").isTextual()) return node.path("raw").asText();
            }
        }
        return null;
    }
}