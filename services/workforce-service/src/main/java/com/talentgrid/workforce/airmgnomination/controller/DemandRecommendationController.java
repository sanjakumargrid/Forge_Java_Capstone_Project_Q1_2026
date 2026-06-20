package com.talentgrid.workforce.airmgnomination.controller;

import com.talentgrid.workforce.airmgnomination.dto.DemandRecommendationResponse;
import com.talentgrid.workforce.airmgnomination.entity.DemandRecommendation;
import com.talentgrid.workforce.airmgnomination.repository.DemandRecommendationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/rmg/nominations/recommendations")
@RequiredArgsConstructor
@Tag(name = "AI Semantic Match Recommendations", description = "Retrieves top AI-matched engineers for a demand")
public class DemandRecommendationController {

    private final DemandRecommendationRepository recommendationRepository;

    @GetMapping("/{demandId}")
    @PreAuthorize("hasAuthority('WORKFORCE_NOMINATION_VIEW')")
    @Operation(summary = "Get top 10 semantic matched engineers",
            description = "Returns the top 10 recommended bench engineers matching the demand's profile using semantic similarity")
    public ResponseEntity<List<DemandRecommendationResponse>> getTopRecommendations(@PathVariable Long demandId) {
        List<DemandRecommendation> recommendations = recommendationRepository
                .findByDemandIdOrderByAiScoreDescAvailabilityDateAsc(demandId);

        List<DemandRecommendationResponse> response = recommendations.stream().map(rec -> {
            var emp = rec.getEmployee();
            return DemandRecommendationResponse.builder()
                    .id(rec.getId())
                    .demandId(rec.getDemandId())
                    .employeeId(emp.getId())
                    .employeeCode(emp.getEmployeeId())
                    .employeeName(emp.getName())
                    .employeeEmail(emp.getEmail())
                    .level(emp.getLevel())
                    .skills(emp.getSkills() != null ? Arrays.asList(emp.getSkills()) : List.of())
                    .availabilityDate(rec.getAvailabilityDate())
                    .aiScore(rec.getAiScore())
                    .createdAt(rec.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
