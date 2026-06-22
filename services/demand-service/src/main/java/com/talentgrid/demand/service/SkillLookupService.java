package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.Skill;
import com.talentgrid.demand.dto.response.SkillDto;
import com.talentgrid.demand.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillLookupService {

    private final SkillRepository skillRepository;

    public List<SkillDto> getAllSkills() {
        return skillRepository.findAllByOrderBySkillNameAsc().stream()
                .map(skill -> SkillDto.builder()
                        .skillId(skill.getSkillId())
                        .skillName(skill.getSkillName())
                        .build())
                .collect(Collectors.toList());
    }

    public List<Skill> resolveSkillIds(List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return List.of();
        }
        List<Skill> skills = skillRepository.findAllById(skillIds);
        if (skills.size() != skillIds.size()) {
            throw new IllegalArgumentException("One or more skill IDs are invalid");
        }
        return skills;
    }
}
