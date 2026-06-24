package com.talentgrid.demand.repository;

import com.talentgrid.demand.domain.entity.AiSkillSuggestionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiSkillSuggestionConfigRepository extends JpaRepository<AiSkillSuggestionConfig, Long> {
    Optional<AiSkillSuggestionConfig> findTopByOrderByIdDesc();
}
