# Demand Analytics Implementation Summary

## Overview
Implemented position-level demand analytics REST API with default 30-day window, supporting:
1. **Demand Fill Rate** (position-level)
2. **Average Time-to-Fill**
3. **Internal vs External Fulfillment Split**

---

## Implementation Details

### 1. New Response DTO
**File:** `DemandAnalyticsMetricsResponse.java`

```json
{
  "metadata": {
    "startDate": "2026-05-15",
    "endDate": "2026-06-14",
    "windowDays": 31
  },
  "fillRate": {
    "totalRequiredPositions": 1000,
    "totalFilledPositions": 800,
    "fillRatePercent": 80.00
  },
  "internalVsExternalSplit": {
    "internalFilledCount": 550,
    "externalFilledCount": 250,
    "internalPercentage": 68.75,
    "externalPercentage": 31.25
  },
  "timeToFill": {
    "averageDaysToFill": 26.45,
    "totalFilledDemands": 80,
    "minDaysToFill": 3.5,
    "maxDaysToFill": 120.0
  }
}
```

### 2. Repository Enhancements
**File:** `DemandRepository.java`

Added position-level analytics queries:
- `sumRequiredPositionsBetween(startDate, endDate)` — total required positions in window
- `sumFilledPositionsBetween(startDate, endDate)` — total filled positions (internal + external)
- `sumInternalFilledCountBetween(startDate, endDate)` — internal fills
- `sumExternalFilledCountBetween(startDate, endDate)` — external fills
- `averageTimeToFillBetween(startDate, endDate)` — avg days from creation to first FILLED_* transition
- `minTimeToFillBetween(startDate, endDate)` — minimum days to fill
- `maxTimeToFillBetween(startDate, endDate)` — maximum days to fill
- `countDemandsWithFilledPositionsBetween(startDate, endDate)` — count of demands with filled positions

**Key Implementation Details:**
- Time-to-fill calculation uses `demand_status_history.changed_at` for accuracy
- Filter: demands where `created_at` is within the date range
- Selects FIRST filled transition per demand using `MIN(history.id)` subquery
- Measures from `demand.created_at` (not `searchStartAt`)

### 3. Service Refactoring
**File:** `DemandAnalyticsService.java`

```java
public DemandAnalyticsMetricsResponse getAnalytics(LocalDate startDate, LocalDate endDate)
```

- **Default window:** Last 30 days (startDate = null → today - 30 days; endDate = null → today)
- **Fill Rate Calculation:** (totalFilledPositions / totalRequiredPositions) * 100
- **Internal/External Percentages:** Calculated from filled position counts
- **Time-to-Fill:** Average/min/max computed from history audit trail
- **Error Handling:** Returns 0.0 for metrics if computation fails (no exceptions thrown)

### 4. Controller Updates
**File:** `DemandAnalyticsController.java`

```
GET /api/v1/analytics/demands?startDate=2026-05-15&endDate=2026-06-14
```

**Parameters:**
- `startDate` (optional, ISO format: yyyy-MM-dd) — default: 30 days ago
- `endDate` (optional, ISO format: yyyy-MM-dd) — default: today
- Authorization: `ANALYTICS_DEMAND_VIEW` (existing authority)

**Response:** `DemandAnalyticsMetricsResponse`



---

## Testing

**Unit Tests:** `DemandAnalyticsServiceTest.java`

Test cases:
1. Default 30-day window calculation
2. Custom date range handling
3. Window days calculation (inclusive)
4. Zero-filled-positions edge case
5. Internal vs external percentage calculations
6. Fill rate percentage rounding

All tests passing ✅

---

## Query Examples

**1. Get analytics for last 30 days (default):**
```
GET /api/v1/analytics/demands
```

**2. Get analytics for specific period:**
```
GET /api/v1/analytics/demands?startDate=2026-05-01&endDate=2026-05-31
```

**3. Get analytics for May 2026:**
```
GET /api/v1/analytics/demands?startDate=2026-05-01&endDate=2026-05-31
```

---

## Database Queries

The implementation uses native SQL for analytics queries, optimized for:
- Minimal joins (demands → demand_status_history)
- Selective aggregations (SUM, AVG, MIN, MAX)
- Date range filtering at SQL level
- First-fill detection via MIN(history.id) subquery

These queries execute on demand without pre-aggregation, suitable for real-time dashboard updates.

---


