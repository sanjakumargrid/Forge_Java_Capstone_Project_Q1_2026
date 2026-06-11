package com.talentgrid.application.application.repository;

import com.talentgrid.application.application.entity.Application;
import com.talentgrid.application.application.enums.Stage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

  boolean existsByCandidateIdAndDemandId(
          Long candidateId,
          Long demandId
  );

  Page<Application> findByDemandDemandId(
          Long demandId,
          Pageable pageable
  );

  Page<Application> findByCurrentStage(
          Stage currentStage,
          Pageable pageable
  );

  Page<Application> findByDemandIdAndCurrentStage(
          Long demandId,
          Stage currentStage,
          Pageable pageable
  );

  Page<Application> findByAiScoreGreaterThanEqual(
          Integer aiScore,
          Pageable pageable
  );
}