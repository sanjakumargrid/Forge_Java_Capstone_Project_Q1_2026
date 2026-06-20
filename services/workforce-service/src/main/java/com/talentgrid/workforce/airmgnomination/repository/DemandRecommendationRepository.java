package com.talentgrid.workforce.airmgnomination.repository;

import com.talentgrid.workforce.airmgnomination.entity.DemandRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandRecommendationRepository extends JpaRepository<DemandRecommendation, Long> {

    List<DemandRecommendation> findByDemandIdOrderByAiScoreDescAvailabilityDateAsc(Long demandId);

    void deleteByDemandId(Long demandId);
}
