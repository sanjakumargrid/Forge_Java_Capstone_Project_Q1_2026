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
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long> {

    /**
     * Finds a non-deleted demand by its ID.
     */
    Optional<Demand> findByDemandIdAndIsDeletedFalse(Long demandId);

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
     * Average time-to-fill in days for CLOSED demands that have a closureReason of FILLED_*.
     * Calculated as the difference between createdAt and updatedAt (closure timestamp).
     */
    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 86400), 0) " +
           "FROM demands WHERE is_deleted = false AND status = 'CLOSED' " +
           "AND closure_reason IN ('FILLED_INTERNAL', 'FILLED_EXTERNAL')", nativeQuery = true)
    double averageTimeToFillDays();
}
