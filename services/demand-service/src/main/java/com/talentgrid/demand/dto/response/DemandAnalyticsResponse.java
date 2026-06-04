package com.talentgrid.demand.dto.response;

/**
 * Analytics response for the demand dashboard.
 * Returned by {@code GET /analytics/demands}.
 *
 * <p>Provides aggregate metrics across all non-deleted demands:
 * <ul>
 *   <li>{@code totalDemands}              — total count of demands</li>
 *   <li>{@code fillRate}                  — percentage of demands reaching FILLED_* states</li>
 *   <li>{@code avgTimeToFillDays}         — average days from creation to first FILLED_* transition</li>
 *   <li>{@code internalFillCount}         — demands resolved via internal search</li>
 *   <li>{@code externalFillCount}         — demands resolved via external search</li>
 *   <li>{@code internalFillPercentage}    — internal vs total filled ratio (%)</li>
 *   <li>{@code externalFillPercentage}    — external vs total filled ratio (%)</li>
 * </ul>
 */
public class DemandAnalyticsResponse {

    private long totalDemands;
    private double fillRate;
    private double avgTimeToFillDays;
    private long internalFillCount;
    private long externalFillCount;
    private double internalFillPercentage;
    private double externalFillPercentage;

    // ─── Getters & Setters ──────────────────────────────────────────────────────
    public long getTotalDemands() { return totalDemands; }
    public void setTotalDemands(long totalDemands) { this.totalDemands = totalDemands; }

    public double getFillRate() { return fillRate; }
    public void setFillRate(double fillRate) { this.fillRate = fillRate; }

    public double getAvgTimeToFillDays() { return avgTimeToFillDays; }
    public void setAvgTimeToFillDays(double avgTimeToFillDays) { this.avgTimeToFillDays = avgTimeToFillDays; }

    public long getInternalFillCount() { return internalFillCount; }
    public void setInternalFillCount(long internalFillCount) { this.internalFillCount = internalFillCount; }

    public long getExternalFillCount() { return externalFillCount; }
    public void setExternalFillCount(long externalFillCount) { this.externalFillCount = externalFillCount; }

    public double getInternalFillPercentage() { return internalFillPercentage; }
    public void setInternalFillPercentage(double internalFillPercentage) { this.internalFillPercentage = internalFillPercentage; }

    public double getExternalFillPercentage() { return externalFillPercentage; }
    public void setExternalFillPercentage(double externalFillPercentage) { this.externalFillPercentage = externalFillPercentage; }
}
