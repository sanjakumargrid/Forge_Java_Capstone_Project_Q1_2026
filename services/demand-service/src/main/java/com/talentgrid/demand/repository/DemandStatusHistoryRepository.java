package com.talentgrid.demand.repository;

import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing demand status transition audit logs.
 */
@Repository
public interface DemandStatusHistoryRepository extends JpaRepository<DemandStatusHistory, Long> {
}
