package com.talentgrid.application.application.repository;

import com.talentgrid.application.application.entity.Application;
import com.talentgrid.application.application.enums.Stage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByCandidateIdAndJobPostingId(Long candidateId, Long jobPostingId);

    List<Application> findByJobPostingIdAndCurrentStageNotIn(Long jobPostingId, List<Stage> stages);

    Page<Application> findByJobPostingId(Long jobPostingId, Pageable pageable);

    Page<Application> findByCurrentStage(Stage currentStage, Pageable pageable);

    Page<Application> findByJobPostingIdAndCurrentStage(
            Long jobPostingId,
            Stage currentStage,
            Pageable pageable
    );

    Page<Application> findByAiScoreGreaterThanEqual(
            Integer aiScore,
            Pageable pageable
    );

    List<Application> findAllByIdIn(List<Long> ids);
}