package com.talentgrid.workforce.rmgnomination.repository;

import com.talentgrid.workforce.rmgnomination.entity.InternalMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternalMatchRepository extends JpaRepository<InternalMatch, Long> {

    boolean existsByEmployee_IdAndDemandIdAndIsDeletedFalse(Long employeeId, Long demandId);

    List<InternalMatch> findByDemandIdAndIsDeletedFalse(Long demandId);

    List<InternalMatch> findByEmployee_IdAndIsDeletedFalse(Long employeeId);
}
