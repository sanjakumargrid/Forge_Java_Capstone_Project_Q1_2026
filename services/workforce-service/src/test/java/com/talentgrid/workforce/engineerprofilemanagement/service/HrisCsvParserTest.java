package com.talentgrid.workforce.engineerprofilemanagement.service;

import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportRowError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HrisCsvParserTest {

    private final HrisCsvParser parser = new HrisCsvParser();

    @Test
    @DisplayName("parses valid bench-style header and one data row")
    void parsesOneRow() throws Exception {
        String csv = """
                Employee ID,Name,Email,Level,Availability Date,Location,Contract Type,Current Project,Utilisation %,Manager ID,HRIS Sync Status,Skills
                E001,Alice,alice@example.com,SENIOR,2026-07-01,London,FULL_TIME,Project A,80,1001,SYNCED,Java|Spring
                """;
        HrisCsvParser.ParseResult result = parser.parse(csv.getBytes(StandardCharsets.UTF_8), 1000);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).hasSize(1);
        HrisImportPreparedRow row = result.rows().get(0);
        assertThat(row.getRowNumber()).isEqualTo(1);
        assertThat(row.getEmployeeId()).isEqualTo("E001");
        assertThat(row.getName()).isEqualTo("Alice");
        assertThat(row.getEmail()).isEqualTo("alice@example.com");
        assertThat(row.getLevelRaw()).isEqualTo("SENIOR");
        assertThat(row.getSkillsRaw()).isEqualTo("Java|Spring");
    }

    @Test
    @DisplayName("returns error when Name column is missing")
    void missingRequiredColumn() throws Exception {
        String csv = """
                Employee ID,Email
                E001,alice@example.com
                """;
        HrisCsvParser.ParseResult result = parser.parse(csv.getBytes(StandardCharsets.UTF_8), 1000);

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors())
                .extracting(HrisImportRowError::getField)
                .contains("file");
    }

    @Test
    @DisplayName("rejects more than maxRows data rows")
    void maxRowsExceeded() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Employee ID,Name,Email\n");
        for (int i = 1; i <= 4; i++) {
            sb.append("E").append(i).append(",N").append(i).append(",e").append(i).append("@x.com\n");
        }
        HrisCsvParser.ParseResult result = parser.parse(sb.toString().getBytes(StandardCharsets.UTF_8), 3);

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors()).anyMatch(e -> e.getMessage().contains("Maximum 3"));
    }

    @Test
    @DisplayName("strips UTF-8 BOM from header")
    void stripsBom() throws Exception {
        String csv = "\uFEFFEmployee ID,Name,Email\nE1,A,a@b.co\n";
        HrisCsvParser.ParseResult result = parser.parse(csv.getBytes(StandardCharsets.UTF_8), 10);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).getEmployeeId()).isEqualTo("E1");
    }
}
