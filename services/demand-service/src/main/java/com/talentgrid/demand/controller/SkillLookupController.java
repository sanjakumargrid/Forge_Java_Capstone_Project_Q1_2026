package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.response.SkillDto;
import com.talentgrid.demand.service.SkillLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lookups/skills")
@RequiredArgsConstructor
public class SkillLookupController {

    private final SkillLookupService skillLookupService;

    @GetMapping
    @PreAuthorize("hasAuthority('DEMAND_VIEW')")
    public List<SkillDto> getAllSkills() {
        return skillLookupService.getAllSkills();
    }
}
