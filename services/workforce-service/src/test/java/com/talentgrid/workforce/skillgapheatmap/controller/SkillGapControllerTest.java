package com.talentgrid.workforce.skillgapheatmap.controller;

import com.talentgrid.shared.auth.security.JwtAuthenticationProvider;
import com.talentgrid.workforce.skillgapheatmap.dto.RefreshResponse;
import com.talentgrid.workforce.skillgapheatmap.dto.SkillGapResponse;
import com.talentgrid.workforce.skillgapheatmap.dto.SkillGapRowDto;
import com.talentgrid.workforce.skillgapheatmap.dto.SkillGapSummaryResponse;
import com.talentgrid.workforce.skillgapheatmap.dto.SkillTrendDto;
import com.talentgrid.workforce.skillgapheatmap.dto.SkillTrendResponse;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SkillGapController.class)
@DisplayName("SkillGapController")
class SkillGapControllerTest {

    private static final String BASE = "/api/v1/skill-gap";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkillGapService skillGapService;

    @MockitoBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @DisplayName("GET /heatmap returns 200 with skill rows")
    @WithMockUser(authorities = "WORKFORCE_SKILLGAP_VIEW")
    void heatmapReturns200() throws Exception {
        when(skillGapService.getSkillGap()).thenReturn(SkillGapResponse.builder()
                .generatedAt(LocalDateTime.parse("2026-06-26T10:00:00"))
                .totalSkills(1)
                .degraded(false)
                .skills(List.of(SkillGapRowDto.builder()
                        .skillName("Java")
                        .demandCount(3)
                        .benchCount(1)
                        .gapScore(2)
                        .gapLevel("MEDIUM")
                        .trendDirection("STABLE")
                        .build()))
                .build());

        mockMvc.perform(get(BASE + "/heatmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSkills").value(1))
                .andExpect(jsonPath("$.skills[0].skillName").value("Java"))
                .andExpect(jsonPath("$.skills[0].demandCount").value(3));
    }

    @Test
    @DisplayName("GET /summary returns 200 with level counts")
    @WithMockUser(authorities = "WORKFORCE_SKILLGAP_VIEW")
    void summaryReturns200() throws Exception {
        when(skillGapService.getSummary()).thenReturn(SkillGapSummaryResponse.builder()
                .totalSkills(4)
                .criticalSkills(1)
                .highSkills(1)
                .mediumSkills(1)
                .lowSkills(1)
                .generatedAt(LocalDateTime.parse("2026-06-26T10:00:00"))
                .degraded(false)
                .build());

        mockMvc.perform(get(BASE + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSkills").value(4))
                .andExpect(jsonPath("$.criticalSkills").value(1));
    }

    @Test
    @DisplayName("GET /trends returns 200 with trend rows")
    @WithMockUser(authorities = "WORKFORCE_SKILLGAP_VIEW")
    void trendsReturns200() throws Exception {
        when(skillGapService.getTrends()).thenReturn(SkillTrendResponse.builder()
                .generatedAt(LocalDateTime.parse("2026-06-26T10:00:00"))
                .degraded(false)
                .trends(List.of(SkillTrendDto.builder()
                        .skillName("Java")
                        .previousGap(1)
                        .currentGap(2)
                        .trendDirection("DOWN")
                        .build()))
                .build());

        mockMvc.perform(get(BASE + "/trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trends[0].skillName").value("Java"))
                .andExpect(jsonPath("$.trends[0].currentGap").value(2));
    }

    @Test
    @DisplayName("POST /refresh returns 200 with SUCCESS status")
    @WithMockUser(authorities = "WORKFORCE_SKILLGAP_REFRESH")
    void refreshReturns200() throws Exception {
        when(skillGapService.refresh()).thenReturn(RefreshResponse.builder()
                .status("SUCCESS")
                .processedSkills(10)
                .refreshedAt(LocalDateTime.parse("2026-06-26T10:00:00"))
                .build());

        mockMvc.perform(post(BASE + "/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.processedSkills").value(10));
    }

    @Test
    @DisplayName("GET /heatmap without auth returns 401 or 403")
    void heatmapRequiresAuth() throws Exception {
        mockMvc.perform(get(BASE + "/heatmap"))
                .andExpect(status().is4xxClientError());
    }
}
