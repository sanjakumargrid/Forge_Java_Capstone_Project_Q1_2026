package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.SeniorityLevelEntity;
import com.talentgrid.demand.dto.response.SeniorityLevelResponse;
import com.talentgrid.demand.repository.SeniorityLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeniorityLevelLookupService {

    private final SeniorityLevelRepository seniorityLevelRepository;

    public List<SeniorityLevelResponse> getAllSeniorityLevels() {
        return seniorityLevelRepository.findAllByOrderByGradeAsc().stream()
                .map(s -> SeniorityLevelResponse.builder()
                        .id(s.getId())
                        .grade(s.getGrade())
                        .displayName(s.getDisplayName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Resolves a grade code (e.g. "T4") to the DB entity.
     * Throws IllegalArgumentException if not found — same pattern as JobTitleLookupService.resolveJobTitleId().
     */
    public SeniorityLevelEntity resolveByGrade(String grade) {
        if (grade == null) {
            return null;
        }
        return seniorityLevelRepository.findByGrade(grade)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid seniority grade: '" + grade + "'"));
    }
}
