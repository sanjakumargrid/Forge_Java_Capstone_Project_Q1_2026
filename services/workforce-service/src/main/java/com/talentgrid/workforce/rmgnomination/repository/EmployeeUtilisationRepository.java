package com.talentgrid.workforce.rmgnomination.repository;

import com.talentgrid.workforce.rmgnomination.entity.EmployeeUtilisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeUtilisationRepository extends JpaRepository<EmployeeUtilisation, Long> {

    @Query("SELECT COALESCE(SUM(eu.allocatedPercentage), 0) FROM EmployeeUtilisation eu " +
            "WHERE eu.employee.id = :employeeId AND eu.isDeleted = false")
    int sumAllocatedPercentageByEmployeeId(@Param("employeeId") Long employeeId);

    List<EmployeeUtilisation> findByEmployee_IdAndIsDeletedFalse(Long employeeId);
    Optional<EmployeeUtilisation> findByEmployee_IdAndDemandIdAndIsDeletedFalse(Long employeeId, Long demandId);
}
