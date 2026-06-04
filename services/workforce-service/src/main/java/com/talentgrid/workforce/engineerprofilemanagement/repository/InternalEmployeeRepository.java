package com.talentgrid.workforce.engineerprofilemanagement.repository;

import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InternalEmployeeRepository extends JpaRepository<InternalEmployee, Long> {

    Optional<InternalEmployee> findByEmployeeIdAndIsDeletedFalse(String employeeId);
}
