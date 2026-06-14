package com.talentgrid.workforce.rmganalyticsdashboard.repository;

import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Analytics-only aggregates for {@link InternalEmployee}.
 */
@Repository
public interface RmgAnalyticsInternalEmployeeRepository extends JpaRepository<InternalEmployee, Long> {

    @Query("SELECT COUNT(e) FROM InternalEmployee e WHERE e.isDeleted = false AND COALESCE(e.utilisationPct, 0) > 0")
    long countBillableEmployees();

    @Query("SELECT COUNT(e) FROM InternalEmployee e WHERE e.isDeleted = false AND COALESCE(e.utilisationPct, 0) = 0")
    long countBenchEmployees();

    @Query("SELECT AVG(COALESCE(e.utilisationPct, 0)) FROM InternalEmployee e WHERE e.isDeleted = false")
    Double averageUtilisationPct();

    /** Bench pool: active employees with zero utilisation (typical “on bench” for RMG charts). */
    @Query("SELECT e FROM InternalEmployee e WHERE e.isDeleted = false AND COALESCE(e.utilisationPct, 0) = 0")
    List<InternalEmployee> findBenchEmployeesForAnalytics();
}
