package com.talentgrid.workforce.skillgapheatmap.provider;

import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandSummary;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts normalized mandatory skill names from demand-service list responses.
 */
final class DemandSkillExtractor {

    private DemandSkillExtractor() {
    }

    static List<String> extractMandatorySkills(DemandSummary demand) {
        if (demand == null) {
            return List.of();
        }

        Set<String> skills = new LinkedHashSet<>();
        addSkillNames(skills, demand.getMandatorySkills());
        return List.copyOf(skills);
    }

    private static void addSkillNames(Set<String> target, List<DemandSummary.SkillDto> skillDtos) {
        if (skillDtos == null || skillDtos.isEmpty()) {
            return;
        }
        for (DemandSummary.SkillDto skillDto : skillDtos) {
            if (skillDto == null || skillDto.getSkillName() == null) {
                continue;
            }
            String normalized = normalizeSkillName(skillDto.getSkillName());
            if (normalized != null) {
                target.add(normalized);
            }
        }
    }

    private static String normalizeSkillName(String skill) {
        if (skill == null) {
            return null;
        }
        String normalized = skill.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
}
