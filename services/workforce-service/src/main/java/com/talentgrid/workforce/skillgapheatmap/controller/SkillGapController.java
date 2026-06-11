package com.talentgrid.workforce.skillgapheatmap.controller;

import com.talentgrid.workforce.skillgapheatmap.dto.RefreshResponse;
import com.talentgrid.workforce.skillgapheatmap.dto.SkillGapResponse;
import com.talentgrid.workforce.skillgapheatmap.dto.SkillGapSummaryResponse;
import com.talentgrid.workforce.skillgapheatmap.dto.SkillTrendResponse;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Skill Gap Heatmap", description = "Skill gap analysis between open demands and bench engineers")
@RestController
@RequestMapping("/api/v1/skill-gap")
@RequiredArgsConstructor
public class SkillGapController {

    private final SkillGapService skillGapService;

    @Operation(summary = "Get full skill gap heatmap", description = "Returns all skills with demand count, bench count, gap score, gap level and trend direction")
    @GetMapping("/heatmap")
    public ResponseEntity<SkillGapResponse> getSkillGap() {
        return ResponseEntity.ok(skillGapService.getSkillGap());
    }

    @Operation(summary = "Get skill gap summary", description = "Returns aggregated counts per gap level (CRITICAL / HIGH / MEDIUM / LOW)")
    @GetMapping("/summary")
    public ResponseEntity<SkillGapSummaryResponse> getSummary() {
        return ResponseEntity.ok(skillGapService.getSummary());
    }

    @Operation(summary = "Get skill trends", description = "Returns skill gap trend direction comparing the two most recent snapshots")
    @GetMapping("/trends")
    public ResponseEntity<SkillTrendResponse> getTrends() {
        return ResponseEntity.ok(skillGapService.getTrends());
    }

    @Operation(summary = "Trigger manual refresh", description = "Recalculates the skill gap heatmap from current demand and bench data")
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh() {
        return ResponseEntity.ok(skillGapService.refresh());
    }
}
