package com.talentgrid.demand.repository;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.DemandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing demand status transition audit logs.
 */
@Repository
public interface DemandStatusHistoryRepository extends JpaRepository<DemandStatusHistory, Long> {

  /**
   * Returns the latest history record where the demand transitioned
   * into the specified status.
   *
   * Example:
   * Used by the Approval SLA scheduler to determine when a demand
   * most recently entered PENDING_APPROVAL.
   */
  Optional<DemandStatusHistory> findTopByDemandDemandIdAndToStatusOrderByChangedAtDesc(
          Long demandId,
          DemandStatus toStatus
  );

  /**
   * Find all edit history entries for a demand (where fromStatus = toStatus)
   * that carry a non-blank reason for the change.
   */
  @Query("SELECT h FROM DemandStatusHistory h WHERE h.demand = :demand AND h.fromStatus = h.toStatus AND h.comments IS NOT NULL AND TRIM(h.comments) <> '' ORDER BY h.changedAt DESC")
  List<DemandStatusHistory> findByDemandAndFromStatusEqualsToStatus(@Param("demand") Demand demand);
}