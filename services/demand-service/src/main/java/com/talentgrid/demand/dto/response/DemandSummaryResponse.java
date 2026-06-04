package com.talentgrid.demand.dto.response;

import java.time.OffsetDateTime;

/**
 * Lightweight demand projection for dashboard and list views.
 * Returned by {@code GET /demands} to avoid loading heavy TEXT/ARRAY columns.
 *
 * <p>Fields chosen to support the dashboard SLA requirement (&lt;2s @ 500 records):
 * only indexed/lightweight columns are included.
 */
public class DemandSummaryResponse {

    private Long demandId;
    private String title;
    private String status;
    private String priority;
    private String businessUnit;

    /** Age of the demand in days from creation to now — computed by the mapper. */
    private Long ageInDays;

    private Integer internalFilledCount;
    private Integer externalFilledCount;
    private Integer requiredCount;

    private OffsetDateTime createdAt;

    // ─── Getters & Setters ──────────────────────────────────────────────────────
    public Long getDemandId() { return demandId; }
    public void setDemandId(Long demandId) { this.demandId = demandId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }

    public Long getAgeInDays() { return ageInDays; }
    public void setAgeInDays(Long ageInDays) { this.ageInDays = ageInDays; }

    public Integer getInternalFilledCount() { return internalFilledCount; }
    public void setInternalFilledCount(Integer internalFilledCount) { this.internalFilledCount = internalFilledCount; }

    public Integer getExternalFilledCount() { return externalFilledCount; }
    public void setExternalFilledCount(Integer externalFilledCount) { this.externalFilledCount = externalFilledCount; }

    public Integer getRequiredCount() { return requiredCount; }
    public void setRequiredCount(Integer requiredCount) { this.requiredCount = requiredCount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
