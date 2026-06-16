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
       Optional<Demand> findByDemandIdAndIsDeletedFalse(Long demandId);

    /**
     * Finds all non-deleted demands with the given status.
     * Used by the Approval SLA scheduler to locate pending approvals.
     */
    List<Demand> findByStatusAndIsDeletedFalse(DemandStatus status);

    /**
     * Enterprise search with optional filters on status, priority, and business unit.
     * Excludes soft-deleted records. Supports pagination and sorting.
     */
    @Query("SELECT d FROM Demand d WHERE d.isDeleted = false " +
            "AND (:status IS NULL OR d.status = :status) " +
            "AND (:priority IS NULL OR d.priority = :priority) " +
            "AND (:businessUnit IS NULL OR d.businessUnit = :businessUnit) " +
            "AND (:accountName IS NULL OR d.accountName = :accountName) " +
            "AND (:location IS NULL OR d.location = :location) " +
            "AND (:employmentType IS NULL OR d.employmentType = :employmentType)")
    Page<Demand> searchDemands(
            @Param("status") DemandStatus status,
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

       /**
        * Sums internal filled counts across all non-deleted demands.
        */
       @Query("SELECT COALESCE(SUM(d.internalFilledCount), 0) FROM Demand d WHERE d.isDeleted = false")
       long sumInternalFilledCount();

       /**
        * Sums external filled counts across all non-deleted demands.
        */
       @Query("SELECT COALESCE(SUM(d.externalFilledCount), 0) FROM Demand d WHERE d.isDeleted = false")
       long sumExternalFilledCount();

       /**
        * Average time-to-fill in days for CLOSED demands that have a closureReason of
        * FILLED_*.
        * Calculated as the difference between createdAt and updatedAt (closure
        * timestamp).
        */

       // ─── Position-Level Analytics Queries (for Dashboard Metrics)
       // ──────────────────

       /**
        * Sum of required positions for demands created or filled in the given date
        * range.
        * Date filtering: WHERE (created_at >= startDate OR demand has filled positions
        * in range)
        */
       @Query(value = "SELECT COALESCE(SUM(d.required_count), 0) FROM demands d " +
                     "WHERE d.is_deleted = false " +
                     "AND (CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                     "  OR (d.internal_filled_count > 0 OR d.external_filled_count > 0))", nativeQuery = true)
       long sumRequiredPositions(@Param("startDate") LocalDate startDate);

       /**
        * Sum of required positions for demands created or filled in the given date
        * range (with end date).
        * Date filtering: WHERE (created_at between startDate and endDate OR demand has
        * filled positions in range)
        */
       @Query(value = "SELECT COALESCE(SUM(d.required_count), 0) FROM demands d " +
                     "WHERE d.is_deleted = false " +
                     "AND (CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                     "  AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE))", nativeQuery = true)
       long sumRequiredPositionsBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

       /**
        * Sum of filled positions (internal + external) for demands in the given date
        * range.
        */
       @Query(value = "SELECT COALESCE(SUM(d.internal_filled_count + d.external_filled_count), 0) " +
                     "FROM demands d " +
                     "WHERE d.is_deleted = false " +
                     "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                     "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE)", nativeQuery = true)
       long sumFilledPositionsBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

       /**
        * Sum of internal filled positions for demands in the given date range.
        */
       @Query(value = "SELECT COALESCE(SUM(d.internal_filled_count), 0) " +
                     "FROM demands d " +
                     "WHERE d.is_deleted = false " +
                     "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                     "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE)", nativeQuery = true)
       long sumInternalFilledCountBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

       /**
        * Sum of external filled positions for demands in the given date range.
        */
       @Query(value = "SELECT COALESCE(SUM(d.external_filled_count), 0) " +
                     "FROM demands d " +
                     "WHERE d.is_deleted = false " +
                     "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                     "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE)", nativeQuery = true)
       long sumExternalFilledCountBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

       /**
        * Average time-to-fill in days for demands that have filled positions in the
        * given date range.
        * Time-to-fill = (earliest FILLED_* status transition timestamp -
        * demand.created_at)
        * Uses demand_status_history to find the exact timestamp of the first FILLED_*
        * transition.
        */
       @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (history.changed_at - d.created_at)) / 86400), 0) " +
                     "FROM demands d " +
                     "INNER JOIN demand_status_history history ON d.demand_id = history.demand_id " +
                     "WHERE d.is_deleted = false " +
                     "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
                     "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE) " +
                     "AND history.to_status IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL', 'FILLED_PARTIALLY') " +
                     "AND history.id = (SELECT MIN(h2.id) FROM demand_status_history h2 WHERE h2.demand_id = d.demand_id "
                     +
                     "  AND h2.to_status IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL', 'FILLED_PARTIALLY'))", nativeQuery = true)
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
                     "AND history.to_status IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL', 'FILLED_PARTIALLY') " +
                     "AND history.id = (SELECT MIN(h2.id) FROM demand_status_history h2 WHERE h2.demand_id = d.demand_id "
                     +
                     "  AND h2.to_status IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL', 'FILLED_PARTIALLY'))", nativeQuery = true)
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
                     "AND history.to_status IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL', 'FILLED_PARTIALLY') " +
                     "AND history.id = (SELECT MIN(h2.id) FROM demand_status_history h2 WHERE h2.demand_id = d.demand_id "
                     +
                     "  AND h2.to_status IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL', 'FILLED_PARTIALLY'))", nativeQuery = true)
       double maxTimeToFillBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Count of demands that have at least one filled position in the given date range.
     */
    @Query(value = "SELECT COUNT(DISTINCT d.demand_id) FROM demands d " +
            "WHERE d.is_deleted = false " +
            "AND (d.internal_filled_count > 0 OR d.external_filled_count > 0) " +
            "AND CAST(d.created_at AS DATE) >= CAST(:startDate AS DATE) " +
            "AND CAST(d.created_at AS DATE) <= CAST(:endDate AS DATE)", nativeQuery = true)
    long countDemandsWithFilledPositionsBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Average time-to-fill in days for CLOSED demands that have a closureReason of
     * FILLED_*.
     * Calculated as the difference between createdAt and updatedAt (closure
     * timestamp).
     */
    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 86400), 0) " +
            "FROM demands WHERE is_deleted = false AND status = 'CLOSED' " +
            "AND closure_reason IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL')", nativeQuery = true)
    double averageTimeToFillDays();
}
