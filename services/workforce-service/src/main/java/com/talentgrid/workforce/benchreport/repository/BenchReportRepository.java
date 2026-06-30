package com.talentgrid.workforce.benchreport.repository;

import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BenchReportRepository extends JpaRepository<InternalEmployee, Long> {

    List<InternalEmployee> findByAvailabilityDateBetweenAndIsDeletedFalseOrderByAvailabilityDateAsc(
            LocalDate start,
            LocalDate end
    );

    /** Includes overdue availability (past dates) through the given end date. */
    List<InternalEmployee> findByAvailabilityDateLessThanEqualAndIsDeletedFalseOrderByAvailabilityDateAsc(
            LocalDate end
    );
}
