package com.talentgrid.workforce.rmgdashboard.controller;

import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSearchRequest;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSearchResponse;
import com.talentgrid.workforce.rmgdashboard.service.DemandSearchService;
import com.talentgrid.workforce.rmgdashboard.service.RmgService;
import com.talentgrid.shared.auth.security.JwtAuthenticationProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RmgDemandStatusController.class)
@DisplayName("RmgDemandStatusController — demand search")
@WithMockUser(authorities = "WORKFORCE_BENCH_SEARCH")
class RmgDemandSearchControllerTest {

    private static final String SEARCH_URL = "/api/v1/rmg/demands/search";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RmgService rmgService;

    @MockitoBean
    private DemandSearchService demandSearchService;

    @MockitoBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @Nested
    @DisplayName("HTTP contract")
    class HttpContract {

        @Test
        @DisplayName("GET /demands/search returns 200 OK")
        void searchReturns200() throws Exception {
            when(demandSearchService.search(any()))
                    .thenReturn(new DemandSearchResponse(List.of(), 0, Instant.now()));

            mockMvc.perform(get(SEARCH_URL))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("response body contains totalResults and results array")
        void responseBodyShape() throws Exception {
            DemandDto dto = DemandDto.builder()
                    .demandId(1L)
                    .title("Java Developer")
                    .build();

            when(demandSearchService.search(any()))
                    .thenReturn(new DemandSearchResponse(List.of(dto), 1, Instant.now()));

            mockMvc.perform(get(SEARCH_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(1))
                    .andExpect(jsonPath("$.results[0].demandId").value(1))
                    .andExpect(jsonPath("$.results[0].title").value("Java Developer"));
        }
    }

    @Nested
    @DisplayName("Query-parameter binding")
    class QueryParamBinding {

        @Test
        @DisplayName("skill and location params are bound to DemandSearchRequest")
        void filterParamsBound() throws Exception {
            when(demandSearchService.search(any()))
                    .thenReturn(new DemandSearchResponse(List.of(), 0, Instant.now()));

            ArgumentCaptor<DemandSearchRequest> captor = ArgumentCaptor.forClass(DemandSearchRequest.class);
            mockMvc.perform(get(SEARCH_URL)
                            .param("skill", "Java")
                            .param("location", "Bangalore")
                            .param("level", "SENIOR")
                            .param("status", "INTERNAL_SEARCH")
                            .param("priority", "HIGH"));

            verify(demandSearchService).search(captor.capture());
            DemandSearchRequest bound = captor.getValue();

            assertThat(bound.getSkill()).isEqualTo("Java");
            assertThat(bound.getLocation()).isEqualTo("Bangalore");
            assertThat(bound.getLevel().name()).isEqualTo("SENIOR");
            assertThat(bound.getStatus()).isEqualTo("INTERNAL_SEARCH");
            assertThat(bound.getPriority()).isEqualTo("HIGH");
        }
    }
}
