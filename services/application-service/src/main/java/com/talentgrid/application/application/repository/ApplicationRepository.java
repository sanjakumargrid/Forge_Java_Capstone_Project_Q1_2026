package com.talentgrid.application.application.repository;

import com.forge.talentAcquisitionEngine.applicationService.application.entity.Application;
import com.forge.talentAcquisitionEngine.applicationService.application.enums.Stage;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

  // =====================================================
  // FETCH APPLICATION WITH INTERVIEWS
  // =====================================================

  @EntityGraph(attributePaths = {
      "candidate",
      "interviews"
  })
  Optional<Application> findWithDetailsById(Long id);

  // =====================================================
  // CANDIDATE APPLICATIONS
  // =====================================================

  List<Application> findByCandidateCandidateId(
      Long candidateId
  );

  Page<Application> findByCandidateCandidateId(
      Long candidateId,
      Pageable pageable
  );

  // =====================================================
  // STAGE FILTER
  // =====================================================

  Page<Application> findByCurrentStage(
      Stage currentStage,
      Pageable pageable
  );

  // =====================================================
  // AI SCORE FILTER
  // =====================================================

  Page<Application> findByAiScoreGreaterThanEqual(
      Integer aiScore,
      Pageable pageable
  );

  // =====================================================
  // REAPPLY VALIDATION
  // =====================================================

  @Query("""
            SELECT a
            FROM Application a
            WHERE a.candidate.candidateId = :candidateId
            AND a.blockedFromReapply = true
            AND a.reapplyAllowedAfter > :currentDateTime
            """)
  List<Application> findBlockedApplications(
      @Param("candidateId") Long candidateId,
      @Param("currentDateTime") LocalDateTime currentDateTime
  );

  // =====================================================
  // ACTIVE APPLICATIONS
  // =====================================================

  @Query("""
            SELECT a
            FROM Application a
            WHERE a.candidate.candidateId = :candidateId
            AND a.currentStage <> 'REJECTED'
            """)
  List<Application> findActiveApplications(
      Long candidateId
  );

  // =====================================================
  // REJECTED APPLICATIONS
  // =====================================================

  Page<Application> findByCurrentStageAndRejectedAtIsNotNull(
      Stage currentStage,
      Pageable pageable
  );

  // =====================================================
  // SEARCH BY SCORE RANGE
  // =====================================================

  Page<Application> findByAiScoreBetween(
      Integer minScore,
      Integer maxScore,
      Pageable pageable
  );

  // =====================================================
  // STAGE + SCORE FILTER
  // =====================================================

  Page<Application> findByCurrentStageAndAiScoreGreaterThanEqual(
      Stage stage,
      Integer score,
      Pageable pageable
  );

  // =====================================================
  // RECENT APPLICATIONS
  // =====================================================

  Page<Application> findByAppliedAtAfter(
      LocalDateTime appliedAfter,
      Pageable pageable
  );

  // =====================================================
  // INTERVIEW PIPELINE
  // =====================================================

  @Query("""
            SELECT a
            FROM Application a
            WHERE a.currentStage IN (
                com.forge.talentAcquisitionEngine.applicationService.application.enums.Stage.INTERVIEW,
                com.forge.talentAcquisitionEngine.applicationService.application.enums.Stage.FINAL_ROUND
            )
            """)
  Page<Application> findInterviewPipeline(
      Pageable pageable
  );

  // =====================================================
  // OFFER PIPELINE
  // =====================================================

  @Query("""
            SELECT a
            FROM Application a
            WHERE a.currentStage =
            com.forge.talentAcquisitionEngine.applicationService.application.enums.Stage.OFFERED
            """)
  Page<Application> findOfferPipeline(
      Pageable pageable
  );

  // =====================================================
  // HIRED CANDIDATES
  // =====================================================

}