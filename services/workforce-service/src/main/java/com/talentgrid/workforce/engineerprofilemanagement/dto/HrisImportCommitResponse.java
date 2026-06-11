package com.talentgrid.workforce.engineerprofilemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrisImportCommitResponse {

    private int totalRows;
    private int created;
    private int updated;
    private int eventsPublished;
    private Instant committedAt;
}
