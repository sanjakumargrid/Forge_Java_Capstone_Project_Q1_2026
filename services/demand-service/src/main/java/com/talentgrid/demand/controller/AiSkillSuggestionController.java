package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.request.AiSkillSuggestionRequest;
import com.talentgrid.demand.dto.response.AiSkillSuggestionResponse;
import com.talentgrid.demand.service.AiSkillSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demands/ai-skill-suggestion")
@RequiredArgsConstructor
public class AiSkillSuggestionController {

    private final AiSkillSuggestionService aiSkillSuggestionService;

    @PostMapping
    @PreAuthorize("hasAuthority('DEMAND_CREATE')")
    public ResponseEntity<AiSkillSuggestionResponse> suggestSkills(
            @Valid @RequestBody AiSkillSuggestionRequest request) {
        
        AiSkillSuggestionResponse response = aiSkillSuggestionService.suggestSkillsForJobDescription(
                request.getJobDescription(),
                request.getJobTitle(),
                request.getLevel(),
                request.getExperienceYears());
        return ResponseEntity.ok(response);
    }
}
