package com.talentgrid.workforce.skillgapheatmap.provider.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Mirrors the fields from demand-service's DemandSummaryResponse
 * (GET /api/v1/demands) — lightweight list projection including mandatory skills.
 *
 * @JsonIgnoreProperties ensures future fields added by the demand-service
 * do not break deserialization in this service.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandSummary {

    private Long demandId;
    private String title;
    private String status;
    private List<SkillDto> mandatorySkills;
    private List<SkillDto> optionalSkills;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkillDto {
        private Long skillId;
        private String skillName;
    }

    // -------------------------------------------------------------------------
    // Pagination wrapper — mirrors the demand-service's DemandListResponse
    // (GET /api/v1/demands returns a paged envelope, not a bare list)
    // -------------------------------------------------------------------------

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PagedResponse {

        private List<DemandSummary> content;

        private int totalPages;

        private long totalElements;

        private boolean last;
    }
}
