package com.talentgrid.workforce.skillgapheatmap.service;

import com.talentgrid.workforce.skillgapheatmap.entity.SkillGapActiveDemand;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandResponse;
import com.talentgrid.workforce.skillgapheatmap.repository.SkillGapActiveDemandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
public class SkillGapActiveDemandRegistry {

    private static final Set<String> ACTIVE_STATUSES = Set.of("INTERNAL_SEARCH", "OPEN_EXTERNAL");

    private final SkillGapActiveDemandRepository repository;

    public boolean isActiveStatus(String status) {
        return status != null && ACTIVE_STATUSES.contains(status);
    }

    @Transactional
    public void upsertActiveDemand(Long demandId, List<String> mandatorySkills) {
        if (demandId == null) {
            return;
        }
        List<String> normalized = normalizeSkills(mandatorySkills);
        if (normalized.isEmpty()) {
            return;
        }
        repository.save(SkillGapActiveDemand.builder()
                .demandId(demandId)
                .mandatorySkills(normalized)
                .build());
    }

    @Transactional
    public void removeDemand(Long demandId) {
        if (demandId != null) {
            repository.deleteById(demandId);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> aggregateDemandSkillCounts() {
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SkillGapActiveDemand demand : repository.findAll()) {
            if (demand.getMandatorySkills() == null) {
                continue;
            }
            for (String skill : demand.getMandatorySkills()) {
                String normalized = normalizeSkillName(skill);
                if (normalized != null) {
                    counts.merge(normalized, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    @Transactional
    public void replaceAll(Collection<DemandResponse> openDemands) {
        repository.deleteAllInBatch();
        if (openDemands == null) {
            return;
        }
        for (DemandResponse demand : openDemands) {
            if (demand == null || demand.getDemandId() == null || demand.getRequiredSkills() == null) {
                continue;
            }
            upsertActiveDemand(demand.getDemandId(), demand.getRequiredSkills());
        }
    }

    private List<String> normalizeSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String skill : skills) {
            String normalized = normalizeSkillName(skill);
            if (normalized != null) {
                unique.add(normalized);
            }
        }
        return List.copyOf(unique);
    }

    private String normalizeSkillName(String skill) {
        if (skill == null) {
            return null;
        }
        String normalized = skill.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
}
