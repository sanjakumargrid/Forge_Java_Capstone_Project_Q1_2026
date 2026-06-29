package com.talentgrid.workforce.engineerprofilemanagement.service;

import com.talentgrid.workforce.engineerprofilemanagement.entity.SkillCatalogEntry;
import com.talentgrid.workforce.engineerprofilemanagement.exception.SkillNotInCatalogException;
import com.talentgrid.workforce.engineerprofilemanagement.repository.SkillCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SkillCatalogValidator {

    private final SkillCatalogRepository skillCatalogRepository;

    public List<String> resolveCanonicalSkills(List<String> requestedSkills) {
        if (requestedSkills == null || requestedSkills.isEmpty()) {
            return List.of();
        }

        List<String> invalid = new ArrayList<>();
        LinkedHashSet<String> canonical = new LinkedHashSet<>();

        for (String skill : requestedSkills) {
            if (skill == null || skill.isBlank()) {
                continue;
            }
            String trimmed = skill.trim();
            Optional<SkillCatalogEntry> entry =
                    skillCatalogRepository.findBySkillNameIgnoreCase(trimmed);
            if (entry.isEmpty()) {
                invalid.add(trimmed);
            } else {
                canonical.add(entry.get().getSkillName());
            }
        }

        if (!invalid.isEmpty()) {
            throw new SkillNotInCatalogException(invalid);
        }

        return List.copyOf(canonical);
    }
}
