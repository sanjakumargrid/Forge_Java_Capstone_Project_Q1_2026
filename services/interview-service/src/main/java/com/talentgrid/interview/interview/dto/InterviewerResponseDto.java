package com.talentgrid.interview.interview.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterviewerResponseDto {
    private Long employeeId;
    private String domainName;
    private String location;
    private String grade;
    
    // Optional: Fields enriched from Employee Service
    private String firstName;
    private String lastName;
    private String email;
}
