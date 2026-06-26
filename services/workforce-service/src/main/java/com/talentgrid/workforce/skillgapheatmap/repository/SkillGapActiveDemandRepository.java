package com.talentgrid.workforce.skillgapheatmap.repository;

import com.talentgrid.workforce.skillgapheatmap.entity.SkillGapActiveDemand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillGapActiveDemandRepository extends JpaRepository<SkillGapActiveDemand, Long> {
}
