package com.talentgrid.demand.repository;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.enums.EmploymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long>, JpaSpecificationExecutor<Demand> {

        /**
         * Finds a non-deleted demand by its ID.
         */
        @Query("SELECT d FROM Demand d LEFT JOIN FETCH d.demandSkills ds LEFT JOIN FETCH ds.skill WHERE d.demandId = :demandId AND d.isDeleted = false")
        Optional<Demand> findByDemandIdAndIsDeletedFalse(@Param("demandId") Long demandId);

        /**
         * Finds all non-deleted demands with the given status.
         * Used by the Approval SLA scheduler to locate pending approvals.
         */
        List<Demand> findByStatusAndIsDeletedFalse(DemandStatus status);

        /**
         * Enterprise search with optional filters on status, priority, and business
         * unit.
         * Excludes soft-deleted records. Supports pagination and sorting.
         * Pass multiple {@code statuses} values to filter with an IN clause.
         */
        @Query("SELECT d FROM Demand d WHERE d.isDeleted = false " +
                        "AND (:statuses IS NULL OR d.status IN :statuses) " +
                        "AND (:priority IS NULL OR d.priority = :priority) " +
                        "AND (:businessUnit IS NULL OR d.businessUnit = :businessUnit) " +
                        "AND (:accountName IS NULL OR d.accountName = :accountName) " +
                        "AND (:location IS NULL OR d.location = :location) " +
                        "AND (:employmentType IS NULL OR d.employmentType = :employmentType)")
        Page<Demand> searchDemands(
                        @Param("statuses") List<DemandStatus> statuses,
                        @Param("priority") DemandPriority priority,
                        @Param("businessUnit") String businessUnit,
                        @Param("accountName") String accountName,
                        @Param("location") String location,
                        @Param("employmentType") EmploymentType employmentType,
                        Pageable pageable);

        /**
         * Counts all non-deleted demands (used by analytics).
         */
        long countByIsDeletedFalse();

        /**
         * Counts non-deleted demands with a specific status.
         */
        long countByStatusAndIsDeletedFalse(DemandStatus status);

        // ─── Position-Level Analytics Queries (for Dashboard Metrics)
        // ──────────────────

        /**
         * Sum of required positions for demands created or filled in the given date
         * range.
         * Date filtering: WHERE (created_at >= startDate OR demand has filled positions
         * in range)
         */
        @Query(value = "SELECT COUNT(d.demand_id) FROM demands d " +
                        "WHERE d.is_deleted = false " +
                        "AND d.is_filled = true " +
                        "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                        "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE)", nativeQuery = true)
        long countFilledDemandsBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Counts demands filled via INTERNAL source in the given date range.
         */
        @Query(value = "SELECT COUNT(d.demand_id) FROM demands d " +
                        "WHERE d.is_deleted = false " +
                        "AND d.is_filled = true AND d.fill_type = 'INTERNAL' " +
                        "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                        "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE)", nativeQuery = true)
        long countFilledDemandsInternalBetween(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Counts demands filled via EXTERNAL source in the given date range.
         */
        @Query(value = "SELECT COUNT(d.demand_id) FROM demands d " +
                        "WHERE d.is_deleted = false " +
                        "AND d.is_filled = true AND d.fill_type = 'EXTERNAL' " +
                        "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                        "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE)", nativeQuery = true)
        long countFilledDemandsExternalBetween(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Total (non-deleted, non-cancelled) demands in the given date range.
         * Used as the denominator for fill rate.
         */
        @Query(value = "SELECT COUNT(d.demand_id) FROM demands d " +
                        "WHERE d.is_deleted = false " +
                        "AND d.status NOT IN ('CANCELLED', 'DUPLICATE') " +
                        "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                        "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE)", nativeQuery = true)
        long countTotalDemandsBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Average time-to-fill in days for demands that have filled positions in the
         * given date range.
         * Time-to-fill = (earliest transition to {@code FILLED} timestamp -
         * demand.created_at)
         * Uses demand_status_history to find the exact timestamp of the first
         * transition to {@code FILLED}.
         */
        @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (history.changed_at - d.created_at)) / 86400), 0) " +
                        "FROM demands d " +
                        "INNER JOIN demand_status_history history ON d.demand_id = history.demand_id " +
                        "WHERE d.is_deleted = false " +
                        "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                        "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE) " +
                        "AND history.to_status = 'FILLED' " +
                        "AND history.id = (SELECT MIN(h2.id) FROM demand_status_history h2 WHERE h2.demand_id = d.demand_id "
                        +
                        "  AND h2.to_status = 'FILLED')", nativeQuery = true)
        double averageTimeToFillBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Minimum days to fill for demands that have filled positions in the given date
         * range.
         */
        @Query(value = "SELECT COALESCE(MIN(EXTRACT(EPOCH FROM (history.changed_at - d.created_at)) / 86400), 0) " +
                        "FROM demands d " +
                        "INNER JOIN demand_status_history history ON d.demand_id = history.demand_id " +
                        "WHERE d.is_deleted = false " +
                        "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                        "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE) " +
                        "AND history.to_status = 'FILLED' " +
                        "AND history.id = (SELECT MIN(h2.id) FROM demand_status_history h2 WHERE h2.demand_id = d.demand_id "
                        +
                        "  AND h2.to_status = 'FILLED')", nativeQuery = true)
        double minTimeToFillBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Maximum days to fill for demands that have filled positions in the given date
         * range.
         */
        @Query(value = "SELECT COALESCE(MAX(EXTRACT(EPOCH FROM (history.changed_at - d.created_at)) / 86400), 0) " +
                        "FROM demands d " +
                        "INNER JOIN demand_status_history history ON d.demand_id = history.demand_id " +
                        "WHERE d.is_deleted = false " +
                        "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                        "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE) " +
                        "AND history.to_status = 'FILLED' " +
                        "AND history.id = (SELECT MIN(h2.id) FROM demand_status_history h2 WHERE h2.demand_id = d.demand_id "
                        +
                        "  AND h2.to_status = 'FILLED')", nativeQuery = true)
        double maxTimeToFillBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Count of demands that are filled (is_filled = true) in the given date range.
         * Used by the time-to-fill analytics to know how many demands were resolved.
         */
        @Query(value = "SELECT COUNT(DISTINCT d.demand_id) FROM demands d " +
                        "WHERE d.is_deleted = false " +
                        "AND d.is_filled = true " +
                        "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                        "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE)", nativeQuery = true)
        long countDemandsWithFilledPositionsBetween(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Average time-to-fill in days for CLOSED demands that have a closureReason of
         * FILLED_*.
         * Calculated as the difference between createdAt and updatedAt (closure
         * timestamp).
         */
        @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 86400), 0) " +
                        "FROM demands WHERE is_deleted = false AND status = 'CLOSED' " +
                        "AND is_filled = true AND fill_type IS NOT NULL", nativeQuery = true)
        double averageTimeToFillDays();

        // ── V1 Analytics Endpoint Queries
        // ────────────────────────────────────────────────

        /**
         * Count demands in scope for fill-rate denominator (non-deleted, created in
         * window).
         */
        @Query(value = "SELECT COUNT(d.demand_id) FROM demands d " +
                        "WHERE d.is_deleted = false " +
                        "AND (:dateFrom IS NULL OR CAST(d.created_at AS DATE) >= CAST(:dateFrom AS DATE)) " +
                        "AND (:dateTo IS NULL OR CAST(d.created_at AS DATE) <= CAST(:dateTo AS DATE)) " +
                        "AND (:businessUnit IS NULL OR d.business_unit = :businessUnit)", nativeQuery = true)
        long countNonCancelledDemands(
                        @Param("dateFrom") LocalDate dateFrom,
                        @Param("dateTo") LocalDate dateTo,
                        @Param("businessUnit") String businessUnit);

        /**
         * Count demands that reached {@code FILLED} at least once (first transition in
         * history).
         */
        @Query(value = "SELECT COUNT(DISTINCT d.demand_id) FROM demands d " +
                        "INNER JOIN demand_status_history h ON d.demand_id = h.demand_id " +
                        "WHERE d.is_deleted = false " +
                        "AND h.to_status = 'FILLED' " +
                        "AND h.id = (SELECT MIN(h2.id) FROM demand_status_history h2 WHERE h2.demand_id = d.demand_id "
                        +
                        "  AND h2.to_status = 'FILLED') " +
                        "AND (:dateFrom IS NULL OR CAST(d.created_at AS DATE) >= CAST(:dateFrom AS DATE)) " +
                        "AND (:dateTo IS NULL OR CAST(d.created_at AS DATE) <= CAST(:dateTo AS DATE)) " +
                        "AND (:businessUnit IS NULL OR d.business_unit = :businessUnit)", nativeQuery = true)
        long countFilledDemands(
                        @Param("dateFrom") LocalDate dateFrom,
                        @Param("dateTo") LocalDate dateTo,
                        @Param("businessUnit") String businessUnit);

        /**
         * Average days to fill: avg(first_filled_status_transition - created_at) for
         * filled demands.
         * Filters: optional dateFrom, dateTo, businessUnit.
         */
        @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (h.changed_at - d.created_at)) / 86400), 0) " +
                        "FROM demands d " +
                        "INNER JOIN demand_status_history h ON d.demand_id = h.demand_id " +
                        "WHERE d.is_deleted = false " +
                        "AND h.to_status = 'FILLED' " +
                        "AND h.id = (SELECT MIN(h2.id) FROM demand_status_history h2 WHERE h2.demand_id = d.demand_id "
                        +
                        "  AND h2.to_status = 'FILLED') " +
                        "AND (:dateFrom IS NULL OR CAST(d.created_at AS DATE) >= CAST(:dateFrom AS DATE)) " +
                        "AND (:dateTo IS NULL OR CAST(d.created_at AS DATE) <= CAST(:dateTo AS DATE)) " +
                        "AND (:businessUnit IS NULL OR d.business_unit = :businessUnit)", nativeQuery = true)
        double getAverageTimeToFillDays(
                        @Param("dateFrom") LocalDate dateFrom,
                        @Param("dateTo") LocalDate dateTo,
                        @Param("businessUnit") String businessUnit);

        /**
         * Filled internally: terminal closed row with {@code fill_type = INTERNAL}.
         */
        @Query(value = "SELECT COUNT(d.demand_id) FROM demands d " +
                        "WHERE d.is_deleted = false " +
                        "AND d.is_filled = true AND d.fill_type = 'INTERNAL' " +
                        "AND (:dateFrom IS NULL OR CAST(d.created_at AS DATE) >= CAST(:dateFrom AS DATE)) " +
                        "AND (:dateTo IS NULL OR CAST(d.created_at AS DATE) <= CAST(:dateTo AS DATE)) " +
                        "AND (:businessUnit IS NULL OR d.business_unit = :businessUnit)", nativeQuery = true)
        long countFilledInternalDemands(
                        @Param("dateFrom") LocalDate dateFrom,
                        @Param("dateTo") LocalDate dateTo,
                        @Param("businessUnit") String businessUnit);

        /**
         * Filled externally: terminal closed row with {@code fill_type = EXTERNAL}.
         */
        @Query(value = "SELECT COUNT(d.demand_id) FROM demands d " +
                        "WHERE d.is_deleted = false " +
                        "AND d.is_filled = true AND d.fill_type = 'EXTERNAL' " +
                        "AND (:dateFrom IS NULL OR CAST(d.created_at AS DATE) >= CAST(:dateFrom AS DATE)) " +
                        "AND (:dateTo IS NULL OR CAST(d.created_at AS DATE) <= CAST(:dateTo AS DATE)) " +
                        "AND (:businessUnit IS NULL OR d.business_unit = :businessUnit)", nativeQuery = true)
        long countFilledExternalDemands(
                        @Param("dateFrom") LocalDate dateFrom,
                        @Param("dateTo") LocalDate dateTo,
                        @Param("businessUnit") String businessUnit);

        /**
         * Capacity by project + client: counts demands grouped by projectId and
         * accountId.
         * Filters: optional dateFrom, dateTo, businessUnit.
         * Returns result as List of Object arrays: [projectId, accountId (as clientId),
         * count]
         */
        @Query(value = "SELECT d.project_id, d.account_id, COUNT(d.demand_id) " +
                        "FROM demands d " +
                        "WHERE d.is_deleted = false " +
                        "AND (:dateFrom IS NULL OR CAST(d.created_at AS DATE) >= CAST(:dateFrom AS DATE)) " +
                        "AND (:dateTo IS NULL OR CAST(d.created_at AS DATE) <= CAST(:dateTo AS DATE)) " +
                        "AND (:businessUnit IS NULL OR d.business_unit = :businessUnit) " +
                        "GROUP BY d.project_id, d.account_id " +
                        "ORDER BY d.project_id, d.account_id", nativeQuery = true)
        List<Object[]> getCapacityByProjectClient(
                        @Param("dateFrom") LocalDate dateFrom,
                        @Param("dateTo") LocalDate dateTo,
                        @Param("businessUnit") String businessUnit);
}
