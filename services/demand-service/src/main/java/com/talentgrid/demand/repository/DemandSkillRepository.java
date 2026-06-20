package com.talentgrid.demand.repository;

import com.talentgrid.demand.domain.entity.DemandSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandSkillRepository extends JpaRepository<DemandSkill, Long> {
    void deleteByDemandDemandId(Long demandId);
}
