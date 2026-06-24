package com.talentgrid.workforce.airmgnomination.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeParsedPayload {

    private Long employeeId;
    private String resumeDriveLink;
    private String parsedText;
    private String sourceUrl;
    private String contentType;
    private Instant parsedAt;
}
