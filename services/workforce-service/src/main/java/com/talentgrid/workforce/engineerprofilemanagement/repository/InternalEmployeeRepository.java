package com.talentgrid.workforce.engineerprofilemanagement.repository;

import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InternalEmployeeRepository extends JpaRepository<InternalEmployee, Long> {

    Optional<InternalEmployee> findByEmployeeIdAndIsDeletedFalse(Long employeeId);

    Optional<InternalEmployee> findByEmailIgnoreCaseAndIsDeletedFalse(String email);

    List<InternalEmployee> findByEmployeeIdInAndIsDeletedFalse(Collection<Long> employeeIds);

    List<InternalEmployee> findByIsDeletedFalseOrderByIdAsc();

    org.springframework.data.domain.Page<InternalEmployee> findByIsDeletedFalse(org.springframework.data.domain.Pageable pageable);

    /**
     * Efficiently updates only the resume embedding and timestamp on an employee
     * without loading the full entity. Returns 1 if the employee was found and updated.
     */
    @Modifying
    @Query("UPDATE InternalEmployee e SET e.resumeEmbedding = :embedding, e.lastEmbeddedAt = :embeddedAt WHERE e.employeeId = :employeeId")
    int updateResumeEmbedding(@Param("employeeId") Long employeeId,
                               @Param("embedding") float[] embedding,
                               @Param("embeddedAt") LocalDateTime embeddedAt);
}

