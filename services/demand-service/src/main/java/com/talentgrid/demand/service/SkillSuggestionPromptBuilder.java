package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.Skill;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SkillSuggestionPromptBuilder {

    public String buildSystemPrompt() {
        return "You are an expert technical recruiter AI. Your task is to analyze a job posting and recommend required and preferred skills. " +
               "CRITICAL: You must ONLY suggest skills from the exact candidate list provided. Do NOT invent or add any skills outside that list. " +
               "Consider the job title, seniority level, and required years of experience when deciding which skills are mandatory vs optional. " +
               "If a required/core skill for the role maps closely to a candidate skill, include it in 'mandatorySkills'. " +
               "If a nice-to-have or complementary skill maps closely, include it in 'optionalSkills'. " +
               "Do NOT put the same skill in both lists. " +
               "Respond ONLY in pure JSON format with no markdown fencing: " +
               "{ \"mandatorySkills\": [{\"skillId\": 1, \"skillName\": \"Java\"}], \"optionalSkills\": [] }";
    }

    public String buildUserPrompt(
            String jobDescriptionText,
            String jobTitle,
            String level,
            Integer experienceYears,
            List<Skill> candidates) {

        StringBuilder context = new StringBuilder();

        if (jobTitle != null && !jobTitle.isBlank()) {
            context.append("Job Title: ").append(jobTitle).append("\n");
        }
        if (level != null && !level.isBlank()) {
            context.append("Seniority Level: ").append(level).append("\n");
        }
        if (experienceYears != null) {
            context.append("Required Experience: ").append(experienceYears).append(" year(s)\n");
        }
        if (context.length() > 0) {
            context.append("\n");
        }

        String candidateListStr = candidates.stream()
                .map(s -> s.getSkillId() + " | " + s.getSkillName())
                .collect(Collectors.joining("\n"));

        return context +
               "Job Description:\n" + jobDescriptionText + "\n\n" +
               "Candidate Skills (ID | Name):\n" + candidateListStr + "\n\n" +
               "Based on the role context and job description above, output the JSON with matching candidate IDs and Names.";
    }
}
