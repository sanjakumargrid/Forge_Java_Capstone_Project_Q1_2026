package com.talentgrid.workforce.rmgsearch.controller;

import com.talentgrid.workforce.benchreport.dto.BenchEmployeeDto;
import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import com.talentgrid.workforce.rmgsearch.dto.RmgSearchRequest;
import com.talentgrid.workforce.rmgsearch.dto.RmgSearchResponse;
import com.talentgrid.workforce.rmgsearch.service.RmgSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.talentgrid.shared.auth.security.JwtAuthenticationProvider;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RmgSearchController.class)
@DisplayName("RmgSearchController")
@WithMockUser(authorities = "WORKFORCE_BENCH_SEARCH")
class RmgSearchControllerTest {

    private static final String SEARCH_URL = "/api/v1/rmg-search/search";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RmgSearchService rmgSearchService;

    @MockitoBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    // -------------------------------------------------------------------------
    // HTTP contract
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("HTTP contract")
    class HttpContract {

        @Test
        @DisplayName("GET /search returns 200 OK")
        void searchReturns200() throws Exception {
            when(rmgSearchService.search(any()))
                    .thenReturn(new RmgSearchResponse(List.of(), 0, Instant.now()));

            mockMvc.perform(get(SEARCH_URL))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("response body contains totalResults and results array")
        void responseBodyShape() throws Exception {
            BenchEmployeeDto dto = new BenchEmployeeDto();
            dto.setEmployeeId(1L);
            dto.setName("Alice");

            when(rmgSearchService.search(any()))
                    .thenReturn(new RmgSearchResponse(List.of(dto), 1, Instant.now()));

            mockMvc.perform(get(SEARCH_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(1))
                    .andExpect(jsonPath("$.results[0].employeeId").value(1))
                    .andExpect(jsonPath("$.results[0].name").value("Alice"));
        }

        @Test
        @DisplayName("empty results return totalResults = 0 and an empty array")
        void emptyResultsBody() throws Exception {
            when(rmgSearchService.search(any()))
                    .thenReturn(new RmgSearchResponse(List.of(), 0, Instant.now()));

            mockMvc.perform(get(SEARCH_URL).param("skill", "Cobol"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(0))
                    .andExpect(jsonPath("$.results").isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // Query-parameter binding
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Query-parameter binding")
    class QueryParamBinding {

        @Test
        @DisplayName("skill param is bound to RmgSearchRequest.skill")
        void skillParamBound() throws Exception {
            when(rmgSearchService.search(any()))
                    .thenReturn(new RmgSearchResponse(List.of(), 0, Instant.now()));

            ArgumentCaptor<RmgSearchRequest> captor = ArgumentCaptor.forClass(RmgSearchRequest.class);
            mockMvc.perform(get(SEARCH_URL).param("skill", "Java"));

            verify(rmgSearchService).search(captor.capture());
            assertThat(captor.getValue().getSkill()).isEqualTo("Java");
        }

        @Test
        @DisplayName("seniority param is converted to Level enum")
        void seniorityParamBound() throws Exception {
            when(rmgSearchService.search(any()))
                    .thenReturn(new RmgSearchResponse(List.of(), 0, Instant.now()));

            ArgumentCaptor<RmgSearchRequest> captor = ArgumentCaptor.forClass(RmgSearchRequest.class);
            mockMvc.perform(get(SEARCH_URL).param("seniority", "SENIOR"));

            verify(rmgSearchService).search(captor.capture());
            assertThat(captor.getValue().getSeniority()).isEqualTo(Level.SENIOR);
        }

        @Test
        @DisplayName("contractType param is converted to ContractType enum")
        void contractTypeParamBound() throws Exception {
            when(rmgSearchService.search(any()))
                    .thenReturn(new RmgSearchResponse(List.of(), 0, Instant.now()));

            ArgumentCaptor<RmgSearchRequest> captor = ArgumentCaptor.forClass(RmgSearchRequest.class);
            mockMvc.perform(get(SEARCH_URL).param("contractType", "CONTRACT"));

            verify(rmgSearchService).search(captor.capture());
            assertThat(captor.getValue().getContractType()).isEqualTo(ContractType.CONTRACT);
        }

        @Test
        @DisplayName("location param is bound to RmgSearchRequest.location")
        void locationParamBound() throws Exception {
            when(rmgSearchService.search(any()))
                    .thenReturn(new RmgSearchResponse(List.of(), 0, Instant.now()));

            ArgumentCaptor<RmgSearchRequest> captor = ArgumentCaptor.forClass(RmgSearchRequest.class);
            mockMvc.perform(get(SEARCH_URL).param("location", "London"));

            verify(rmgSearchService).search(captor.capture());
            assertThat(captor.getValue().getLocation()).isEqualTo("London");
        }

        @Test
        @DisplayName("availabilityDateFrom param is parsed as LocalDate (ISO format)")
        void availabilityDateFromParamBound() throws Exception {
            when(rmgSearchService.search(any()))
                    .thenReturn(new RmgSearchResponse(List.of(), 0, Instant.now()));

            ArgumentCaptor<RmgSearchRequest> captor = ArgumentCaptor.forClass(RmgSearchRequest.class);
            mockMvc.perform(get(SEARCH_URL).param("availabilityDateFrom", "2026-06-01"));

            verify(rmgSearchService).search(captor.capture());
            assertThat(captor.getValue().getAvailabilityDateFrom())
                    .isEqualTo(LocalDate.of(2026, 6, 1));
        }

        @Test
        @DisplayName("availabilityDateTo param is parsed as LocalDate (ISO format)")
        void availabilityDateToParamBound() throws Exception {
            when(rmgSearchService.search(any()))
                    .thenReturn(new RmgSearchResponse(List.of(), 0, Instant.now()));

            ArgumentCaptor<RmgSearchRequest> captor = ArgumentCaptor.forClass(RmgSearchRequest.class);
            mockMvc.perform(get(SEARCH_URL).param("availabilityDateTo", "2026-09-30"));

            verify(rmgSearchService).search(captor.capture());
            assertThat(captor.getValue().getAvailabilityDateTo())
                    .isEqualTo(LocalDate.of(2026, 9, 30));
        }

        @Test
        @DisplayName("all five filter params are bound correctly in a single request")
        void allParamsBoundTogether() throws Exception {
            when(rmgSearchService.search(any()))
                    .thenReturn(new RmgSearchResponse(List.of(), 0, Instant.now()));

            ArgumentCaptor<RmgSearchRequest> captor = ArgumentCaptor.forClass(RmgSearchRequest.class);

            mockMvc.perform(get(SEARCH_URL)
                            .param("skill", "Java")
                            .param("location", "London")
                            .param("seniority", "MID")
                            .param("contractType", "CONTRACT")
                            .param("availabilityDateFrom", "2026-06-01")
                            .param("availabilityDateTo", "2026-09-01"))
                    .andExpect(status().isOk());

            verify(rmgSearchService).search(captor.capture());
            RmgSearchRequest bound = captor.getValue();

            assertThat(bound.getSkill()).isEqualTo("Java");
            assertThat(bound.getLocation()).isEqualTo("London");
            assertThat(bound.getSeniority()).isEqualTo(Level.MID);
            assertThat(bound.getContractType()).isEqualTo(ContractType.CONTRACT);
            assertThat(bound.getAvailabilityDateFrom()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(bound.getAvailabilityDateTo()).isEqualTo(LocalDate.of(2026, 9, 1));
        }

        @Test
        @DisplayName("omitting all params sends a request with all fields null")
        void noParamsSendsNullFields() throws Exception {
            when(rmgSearchService.search(any()))
                    .thenReturn(new RmgSearchResponse(List.of(), 0, Instant.now()));

            ArgumentCaptor<RmgSearchRequest> captor = ArgumentCaptor.forClass(RmgSearchRequest.class);
            mockMvc.perform(get(SEARCH_URL));

            verify(rmgSearchService).search(captor.capture());
            RmgSearchRequest bound = captor.getValue();

            assertThat(bound.getSkill()).isNull();
            assertThat(bound.getLocation()).isNull();
            assertThat(bound.getSeniority()).isNull();
            assertThat(bound.getContractType()).isNull();
            assertThat(bound.getAvailabilityDateFrom()).isNull();
            assertThat(bound.getAvailabilityDateTo()).isNull();
        }
    }
}
