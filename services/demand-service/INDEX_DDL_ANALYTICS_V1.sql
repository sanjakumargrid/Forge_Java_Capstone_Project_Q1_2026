-- ============================================================================
-- INDEX DDL FOR V1 ANALYTICS ENDPOINT PERFORMANCE OPTIMIZATION
-- ============================================================================
--
-- Database: PostgreSQL (TalentGrid)
-- Table: demands
-- Related table: demand_status_history
--
-- Purpose: Optimize query performance for GET /api/v1/demands/analytics
-- Target p95 response time: < 500ms
--
-- ============================================================================

-- ─────────────────────────────────────────────────────────────────────────
-- INDEX 1: Business Unit + Creation Date (Recommended - Composite)
-- ─────────────────────────────────────────────────────────────────────────
-- Rationale:
--   - Most queries filter by business_unit and created_at date range
--   - Composite index allows single index scan for both conditions
--   - Reduces table scans significantly for large datasets
--
-- SQL:
CREATE INDEX idx_demands_business_unit_created_at
  ON demands(business_unit, created_at DESC)
  WHERE is_deleted = false;

-- Alternative without DESC if database prefers:
-- CREATE INDEX idx_demands_business_unit_created_at
--   ON demands(business_unit, created_at)
--   WHERE is_deleted = false;

-- ─────────────────────────────────────────────────────────────────────────
-- INDEX 2: Closure Reason (Recommended - New)
-- ─────────────────────────────────────────────────────────────────────────
-- Rationale:
--   - Closure reason filtering for internalVsExternalSplit metric
--   - Currently not indexed; table scans for closure_reason filter
--   - Values: 'FILLED_INTERNAL', 'FILLED_EXTERNAL', 'CANCELLED', 'ON_HOLD', 'DUPLICATE'
--
-- SQL:
CREATE INDEX idx_demands_closure_reason
  ON demands(closure_reason)
  WHERE is_deleted = false AND status IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL', 'FILLED_PARTIALLY', 'CLOSED');

-- ─────────────────────────────────────────────────────────────────────────
-- INDEX 3: Project ID + Account ID (Recommended - Composite for Capacity)
-- ─────────────────────────────────────────────────────────────────────────
-- Rationale:
--   - capacityByProjectClient metric requires GROUP BY project_id, account_id
--   - Composite index covers both columns for index-only scans
--   - Significant performance improvement for grouping operations
--
-- SQL:
CREATE INDEX idx_demands_project_client
  ON demands(project_id, account_id)
  WHERE is_deleted = false;

-- ─────────────────────────────────────────────────────────────────────────
-- INDEX 4: Status for Analytics Filtering (Enhancement)
-- ─────────────────────────────────────────────────────────────────────────
-- Rationale:
--   - Status filtering is already partially covered by idx_demands_filter
--   - This separate index can be used for dedicated status scans
--   - Useful for COUNT queries that filter by status alone
--   - Index already exists in @Entity definition, but documenting for clarity
--
-- Note: Already part of idx_demands_filter (status, priority, business_unit, ...)
--       No additional index needed; existing index is sufficient.

-- ─────────────────────────────────────────────────────────────────────────
-- DEMAND_STATUS_HISTORY INDEXES (For avgTimeToFillDays calculation)
-- ─────────────────────────────────────────────────────────────────────────
-- Rationale:
--   - avgTimeToFillDays query joins demands with demand_status_history
--   - Filters by to_status IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL', 'FILLED_PARTIALLY')
--   - Currently no specific index; table scan on status history
--
-- SQL:
CREATE INDEX idx_demand_status_history_status_changed_at
  ON demand_status_history(demand_id, to_status, changed_at);

-- Alternative with just status filter:
-- CREATE INDEX idx_demand_status_history_to_status
--   ON demand_status_history(to_status);

-- ─────────────────────────────────────────────────────────────────────────
-- VERIFICATION QUERIES
-- ─────────────────────────────────────────────────────────────────────────

-- Check existing indexes on demands table:
SELECT
  indexname,
  indexdef
FROM pg_indexes
WHERE tablename = 'demands'
ORDER BY indexname;

-- Check for missing indexes:
SELECT * FROM pg_stat_user_indexes
WHERE relname = 'demands'
ORDER BY idx_scan DESC;

-- ─────────────────────────────────────────────────────────────────────────
-- QUERY EXECUTION PLAN ANALYSIS
-- ─────────────────────────────────────────────────────────────────────────

-- After creating indexes, run EXPLAIN ANALYZE on these representative queries:

-- 1. Fill rate query:
EXPLAIN ANALYZE
SELECT COUNT(d.demand_id) FROM demands d
WHERE d.is_deleted = false
AND d.status NOT IN ('CANCELLED', 'DUPLICATE')
AND CAST(d.created_at AS DATE) >= CAST('2026-05-22' AS DATE)
AND CAST(d.created_at AS DATE) <= CAST('2026-06-21' AS DATE)
AND d.business_unit = 'Engineering';

-- 2. Average time-to-fill query (with status history join):
EXPLAIN ANALYZE
SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (h.changed_at - d.created_at)) / 86400), 0)
FROM demands d
INNER JOIN demand_status_history h ON d.demand_id = h.demand_id
WHERE d.is_deleted = false
AND h.to_status IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL', 'FILLED_PARTIALLY')
AND h.id = (SELECT MIN(h2.id) FROM demand_status_history h2 WHERE h2.demand_id = d.demand_id
  AND h2.to_status IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL', 'FILLED_PARTIALLY'))
AND CAST(d.created_at AS DATE) >= CAST('2026-05-22' AS DATE)
AND CAST(d.created_at AS DATE) <= CAST('2026-06-21' AS DATE')
AND d.business_unit = 'Engineering';

-- 3. Capacity grouping query:
EXPLAIN ANALYZE
SELECT d.project_id, d.account_id, COUNT(d.demand_id)
FROM demands d
WHERE d.is_deleted = false
AND CAST(d.created_at AS DATE) >= CAST('2026-05-22' AS DATE)
AND CAST(d.created_at AS DATE) <= CAST('2026-06-21' AS DATE')
AND d.business_unit = 'Engineering'
GROUP BY d.project_id, d.account_id
ORDER BY d.project_id, d.account_id;

-- ═════════════════════════════════════════════════════════════════════════
-- PERFORMANCE BASELINE
-- ═════════════════════════════════════════════════════════════════════════
-- Record these metrics BEFORE and AFTER index creation:
--   - Query execution time (ms)
--   - Rows scanned
--   - Index usage (check pg_stat_user_indexes.idx_scan)
--   - Disk I/O operations

-- POST-INDEX TUNING (if needed):
--   - VACUUM ANALYZE demands; -- Refresh table statistics
--   - REINDEX INDEX idx_demands_business_unit_created_at;
--   - Monitor pg_stat_user_indexes for index bloat

-- ═════════════════════════════════════════════════════════════════════════

