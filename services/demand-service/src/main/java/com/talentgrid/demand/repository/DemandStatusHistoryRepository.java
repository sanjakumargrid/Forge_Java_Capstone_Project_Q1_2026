package com.talentgrid.demand.repository;

import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.DemandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}