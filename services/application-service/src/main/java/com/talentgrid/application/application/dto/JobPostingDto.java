package com.talentgrid.application.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobPostingDto {

    @JsonProperty("job_id")
    @JsonAlias({"id", "jobPostingId", "job_posting_id"})
    private Long jobPostingId;

    @JsonProperty("demand_id")
    private Long demandId;

    @JsonProperty("role_title")
    private String title;

    private String description;

    private String responsibilities;

    private String requirements;

    private String benefits;

    @JsonProperty("employment_type")
    private String employmentType;

    @JsonProperty("experience_level")
    private String level;

    @JsonProperty("experience_years")
    private Double experienceYears;

    @JsonProperty("work_mode")
    private String workMode;

    @JsonProperty("location_city")
    private String locationCity;

    @JsonProperty("location_state")
    private String locationState;

    @JsonProperty("location_country")
    private String locationCountry;

    private String department;

    @JsonProperty("job_category")
    private String jobCategory;

    @JsonProperty("skills_required")
    private List<String> skills;

    private BigDecimal budget;

    @JsonProperty("required_count")
    private Integer requiredCount;

    private String currency;

    @JsonProperty("salary_min")
    private BigDecimal salaryMin;

    @JsonProperty("salary_max")
    private BigDecimal salaryMax;

    @JsonProperty("show_salary")
    private Boolean showSalary;

    @JsonProperty("application_deadline")
    private LocalDate applicationDeadline;

    @JsonProperty("posting_status")
    private String postingStatus;

    @JsonProperty("approval_status")
    private String approvalStatus;

    @JsonProperty("published_at")
    private LocalDateTime publishedAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public String getDisplayLocation() {
        return Stream.of(locationCity, locationState, locationCountry)
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
    }
}