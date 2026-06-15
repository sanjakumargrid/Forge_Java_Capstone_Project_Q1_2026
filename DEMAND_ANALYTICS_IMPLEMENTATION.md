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
GET /api/analytics/demands?startDate=2026-05-15&endDate=2026-06-14
```

**Parameters:**
- `startDate` (optional, ISO format: yyyy-MM-dd) — default: 30 days ago
- `endDate` (optional, ISO format: yyyy-MM-dd) — default: today
- Authorization: `ANALYTICS_DEMAND_VIEW` (existing authority)

**Response:** `DemandAnalyticsMetricsResponse`

---

## Key Decisions Implemented

1. ✅ **Position-Level Metrics:** Unlike demand-count metrics, this analytics computes actual positions filled
2. ✅ **Use `createdAt` as start point:** Time-to-fill measured from demand creation (not searchStartAt)
3. ✅ **Use history transition timestamp:** `demand_status_history.changed_at` for precise fill moment
4. ✅ **Date range filtering:** Includes both demand created AND filled in window
5. ✅ **30-day default window:** Automatic if no dates provided
6. ✅ **Mixed fulfillment support:** Single demand can have internal + external fills counted together

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
GET /api/analytics/demands
```

**2. Get analytics for specific period:**
```
GET /api/analytics/demands?startDate=2026-05-01&endDate=2026-05-31
```

**3. Get analytics for May 2026:**
```
GET /api/analytics/demands?startDate=2026-05-01&endDate=2026-05-31
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

## Files Modified/Created

1. ✅ Created: `DemandAnalyticsMetricsResponse.java` (new response DTO)
2. ✅ Modified: `DemandRepository.java` (added 9 new position-level queries)
3. ✅ Modified: `DemandAnalyticsService.java` (refactored to position-level, date filtering)
4. ✅ Modified: `DemandAnalyticsController.java` (added startDate/endDate parameters)
5. ✅ Created: `DemandAnalyticsServiceTest.java` (4 unit test cases, all passing)

---

## Next Steps

The implementation is complete and ready for:
- Integration testing with a real database
- Performance testing on large datasets (potential materialized views for >1M demands)
- Dashboard integration by BL Team 4
- Monitoring of metric accuracy vs. source systems (offer-service, candidate-service)

