package com.talentgrid.workforce.rmganalyticsdashboard.controller;

import com.talentgrid.workforce.rmganalyticsdashboard.dto.OpenDemandsResponse;
import com.talentgrid.workforce.rmganalyticsdashboard.service.RmgAnalyticsDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RmgAnalyticsDashboardController.class)
@DisplayName("RmgAnalyticsDashboardController")
class RmgAnalyticsDashboardControllerTest {

    private static final String BASE = "/api/v1/rmg-analytics-dashboard";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RmgAnalyticsDashboardService analyticsDashboardService;

    @Test
    @DisplayName("GET /open-demands returns 200 and openCount")
    void openDemandsReturns200() throws Exception {
        when(analyticsDashboardService.getCurrentOpenDemands()).thenReturn(
                OpenDemandsResponse.builder()
                        .openCount(5)
                        .build());

        mockMvc.perform(get(BASE + "/open-demands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCount").value(5));
    }

    @Test
    @DisplayName("GET /internal-match-success-rate passes sinceDays query param")
    void successRatePassesSinceDays() throws Exception {
        when(analyticsDashboardService.getInternalMatchSuccessRate(30))
                .thenReturn(com.talentgrid.workforce.rmganalyticsdashboard.dto.InternalMatchSuccessRateResponse.builder()
                        .rate(BigDecimal.valueOf(80))
                        .successfulMatches(8)
                        .totalDecidedMatches(10)
                        .build());

        mockMvc.perform(get(BASE + "/internal-match-success-rate").param("sinceDays", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successfulMatches").value(8))
                .andExpect(jsonPath("$.totalDecidedMatches").value(10));
    }
}
