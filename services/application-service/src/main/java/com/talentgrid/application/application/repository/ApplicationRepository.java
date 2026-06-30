package com.talentgrid.application.application.repository;

import com.talentgrid.application.application.entity.Application;
import com.talentgrid.application.application.enums.Stage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByCandidateIdAndJobPostingId(
            Long candidateId,
            Long jobPostingId
    );

    boolean existsByCandidateIdAndJobPostingIdAndDemandId(
            Long candidateId,
            Long jobPostingId,
            Long demandId
    );

    List<Application> findByDemandIdAndCurrentStage(
            Long demandId,
            Stage currentStage
    );

    Optional<Application> findFirstByCandidateIdAndJobPostingIdAndDemandId(
            Long candidateId,
            Long jobPostingId,
            Long demandId
    );

    List<Application> findByCandidateIdAndJobPostingIdAndDemandId(
            Long candidateId,
            Long jobPostingId,
            Long demandId
    );

    List<Application> findByCandidateIdAndDemandId(
            Long candidateId,
            Long demandId
    );

    List<Application> findByDemandId(
            Long demandId
    );

    List<Application> findByJobPostingIdAndCurrentStageNotIn(
            Long jobPostingId,
            List<Stage> stages
    );

    Page<Application> findByJobPostingId(
            Long jobPostingId,
            Pageable pageable
    );

    Page<Application> findByCurrentStage(
            Stage currentStage,
            Pageable pageable
    );

    Page<Application> findByJobPostingIdAndCurrentStage(
            Long jobPostingId,
            Stage currentStage,
            Pageable pageable
    );

    Page<Application> findByAiScoreGreaterThanEqual(
            Integer aiScore,
            Pageable pageable
    );

    List<Application> findAllByIdIn(
            List<Long> ids
    );

    List<Application> findByCandidateIdAndJobPostingId(
            Long candidateId,
            Long jobPostingId
    );
}