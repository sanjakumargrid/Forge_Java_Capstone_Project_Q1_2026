package com.talentgrid.demand.repository;

import com.talentgrid.demand.domain.entity.SeniorityLevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeniorityLevelRepository extends JpaRepository<SeniorityLevelEntity, Long> {

    List<SeniorityLevelEntity> findAllByOrderByGradeAsc();

    Optional<SeniorityLevelEntity> findByGrade(String grade);
}
