package com.talentgrid.workforce.aiupskill.repository;

import com.talentgrid.workforce.aiupskill.entity.UpskillHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UpskillHistoryRepository extends JpaRepository<UpskillHistoryEntity, UUID> {

    // Finds the most recent cached learning path execution
    Optional<UpskillHistoryEntity> findFirstByEmployeeIdAndDemandIdOrderByGeneratedAtDesc(String employeeId, String demandId);

    // Pulls down the historical timeline audit trace for a specific user profile
    List<UpskillHistoryEntity> findByEmployeeIdOrderByGeneratedAtDesc(String employeeId);
}