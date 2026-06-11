package com.talentgrid.workforce.engineerprofilemanagement.controller;

import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportCommitResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportValidationResponse;
import com.talentgrid.workforce.engineerprofilemanagement.service.HrisImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HrisImportController.class)
@DisplayName("HrisImportController")
class HrisImportControllerTest {

    private static final String CSV = """
            Employee ID,Name,Email
            E1,Test,test@example.com
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HrisImportService hrisImportService;

    @Test
    @DisplayName("POST without commit returns validation only")
    void validateOnlyDefaultCommitFalse() throws Exception {
        HrisImportValidationResponse validation = HrisImportValidationResponse.builder()
                .totalRows(1)
                .validRows(1)
                .invalidRows(0)
                .canCommit(true)
                .errors(List.of())
                .build();

        when(hrisImportService.importCsv(any(), eq(false), eq(null))).thenReturn(
                HrisImportResponse.builder()
                        .commitRequested(false)
                        .validation(validation)
                        .commitResult(null)
                        .build());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.csv",
                "text/csv",
                CSV.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/engineer-profile/hris-import")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commitRequested").value(false))
                .andExpect(jsonPath("$.validation.totalRows").value(1))
                .andExpect(jsonPath("$.validation.canCommit").value(true))
                .andExpect(jsonPath("$.commitResult").doesNotExist());

        ArgumentCaptor<Boolean> commitCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(hrisImportService).importCsv(any(), commitCaptor.capture(), eq(null));
        assertThat(commitCaptor.getValue()).isFalse();
    }

    @Test
    @DisplayName("POST with commit=true returns validation and commitResult")
    void commitTrueReturnsUnifiedBody() throws Exception {
        HrisImportValidationResponse validation = HrisImportValidationResponse.builder()
                .totalRows(1)
                .validRows(1)
                .invalidRows(0)
                .canCommit(true)
                .errors(List.of())
                .build();

        HrisImportCommitResponse commit = HrisImportCommitResponse.builder()
                .totalRows(1)
                .created(1)
                .updated(0)
                .eventsPublished(1)
                .committedAt(Instant.parse("2026-06-10T12:00:00Z"))
                .build();

        when(hrisImportService.importCsv(any(), eq(true), eq("req-123"))).thenReturn(
                HrisImportResponse.builder()
                        .commitRequested(true)
                        .validation(validation)
                        .commitResult(commit)
                        .build());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.csv",
                MediaType.TEXT_PLAIN_VALUE,
                CSV.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/engineer-profile/hris-import")
                        .file(file)
                        .param("commit", "true")
                        .header("X-Request-Id", "req-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commitRequested").value(true))
                .andExpect(jsonPath("$.commitResult.created").value(1))
                .andExpect(jsonPath("$.commitResult.eventsPublished").value(1));

        verify(hrisImportService).importCsv(any(), eq(true), eq("req-123"));
    }
}
