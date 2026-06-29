package com.talentgrid.workforce.engineerprofilemanagement.exception;

import java.util.List;

public class SkillNotInCatalogException extends RuntimeException {

    private final List<String> invalidSkills;

    public SkillNotInCatalogException(List<String> invalidSkills) {
        super(buildMessage(invalidSkills));
        this.invalidSkills = List.copyOf(invalidSkills);
    }

    public List<String> getInvalidSkills() {
        return invalidSkills;
    }

    private static String buildMessage(List<String> invalidSkills) {
        if (invalidSkills == null || invalidSkills.isEmpty()) {
            return "One or more skills do not exist in the catalog.";
        }
        if (invalidSkills.size() == 1) {
            return "Skill '" + invalidSkills.get(0) + "' does not exist in the catalog.";
        }
        return "Skills " + invalidSkills + " do not exist in the catalog.";
    }
}
