package com.talentgrid.interview.scorecard.service;

import com.talentgrid.interview.client.ApplicationClient;
import com.talentgrid.interview.exception.BusinessException;
import com.talentgrid.interview.interview.entity.Interview;
import com.talentgrid.interview.interview.enums.Status;
import com.talentgrid.interview.interview.repository.InterviewRepository;
import com.talentgrid.interview.scorecard.dto.ScorecardRequestDto;
import com.talentgrid.interview.scorecard.dto.ScorecardResponseDto;
import com.talentgrid.interview.scorecard.dto.ScorecardSummaryDto;
import com.talentgrid.interview.scorecard.entity.Scorecard;
import com.talentgrid.interview.scorecard.enums.Recommendation;
import com.talentgrid.interview.scorecard.repository.ScorecardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScorecardService {

    private final ScorecardRepository scorecardRepository;

    private final InterviewRepository interviewRepository;

    private final ApplicationClient applicationClient;

    @Transactional
    public ScorecardResponseDto submitScorecard(
            Long interviewId,
            ScorecardRequestDto requestDto
    ) {

        Interview interview =
                interviewRepository.findById(interviewId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND,
                                        "Interview not found with id: " + interviewId
                                )
                        );

        if (interview.getStatus() != Status.COMPLETED) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Scorecard can be submitted only after interview is completed"
            );
        }

        boolean interviewerAssigned =
                interview.getInterviewers() != null
                        && interview.getInterviewers()
                        .stream()
                        .anyMatch(interviewer ->
                                Objects.equals(
                                        interviewer.getEmployeeId(),
                                        requestDto.getInterviewerId()
                                )
                        );

        if (!interviewerAssigned) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Interviewer " + requestDto.getInterviewerId()
                            + " is not assigned to interview " + interviewId
            );
        }

        boolean alreadySubmitted =
                scorecardRepository.existsByInterview_InterviewIdAndInterviewerId(
                        interviewId,
                        requestDto.getInterviewerId()
                );

        if (alreadySubmitted) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Scorecard already submitted by interviewer "
                            + requestDto.getInterviewerId()
                            + " for interview " + interviewId
            );
        }

        Double averageScore =
                calculateAverageScore(requestDto);

        Scorecard scorecard = new Scorecard();

        scorecard.setInterview(interview);
        scorecard.setApplicationId(interview.getApplicationId());
        scorecard.setInterviewerId(requestDto.getInterviewerId());
        scorecard.setTechnicalScore(requestDto.getTechnicalScore());
        scorecard.setCommunicationScore(requestDto.getCommunicationScore());
        scorecard.setProblemSolvingScore(requestDto.getProblemSolvingScore());
        scorecard.setCultureFitScore(requestDto.getCultureFitScore());
        scorecard.setAverageScore(averageScore);
        scorecard.setRecommendation(requestDto.getRecommendation());
        scorecard.setOverallFeedback(requestDto.getOverallFeedback());

        Scorecard savedScorecard =
                scorecardRepository.save(scorecard);

        moveApplicationStageBasedOnRecommendation(
                interview.getApplicationId(),
                requestDto.getRecommendation()
        );

        return toResponseDto(savedScorecard);
    }

    @Transactional(readOnly = true)
    public ScorecardResponseDto getScorecardById(Long scorecardId) {

        Scorecard scorecard =
                scorecardRepository.findById(scorecardId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND,
                                        "Scorecard not found with id: " + scorecardId
                                )
                        );

        return toResponseDto(scorecard);
    }

    @Transactional(readOnly = true)
    public List<ScorecardSummaryDto> getScorecardsByInterview(Long interviewId) {

        if (!interviewRepository.existsById(interviewId)) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Interview not found with id: " + interviewId
            );
        }

        return scorecardRepository.findAllByInterview_InterviewId(interviewId)
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScorecardSummaryDto> getScorecardsByApplication(Long applicationId) {

        return scorecardRepository.findAllByApplicationId(applicationId)
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScorecardResponseDto getScorecardByInterviewer(
            Long interviewId,
            Long interviewerId
    ) {

        if (!interviewRepository.existsById(interviewId)) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Interview not found with id: " + interviewId
            );
        }

        Scorecard scorecard =
                scorecardRepository.findByInterview_InterviewIdAndInterviewerId(
                                interviewId,
                                interviewerId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND,
                                        "Scorecard not found for interviewer "
                                                + interviewerId
                                                + " in interview "
                                                + interviewId
                                )
                        );

        return toResponseDto(scorecard);
    }

    @Transactional
    public void deleteScorecard(Long scorecardId) {

        Scorecard scorecard =
                scorecardRepository.findById(scorecardId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND,
                                        "Scorecard not found with id: " + scorecardId
                                )
                        );

        scorecardRepository.delete(scorecard);
    }

    private void moveApplicationStageBasedOnRecommendation(
            Long applicationId,
            Recommendation recommendation
    ) {

        if (recommendation == null) {
            return;
        }

        try {
            if (recommendation == Recommendation.STRONG_HIRE
                    || recommendation == Recommendation.HIRE) {

                applicationClient.moveApplicationStage(
                        applicationId,
                        "FINAL_ROUND",
                        "Interview scorecard recommendation: " + recommendation
                );
            }

            if (recommendation == Recommendation.NO_HIRE
                    || recommendation == Recommendation.STRONG_NO_HIRE) {

                applicationClient.moveApplicationStage(
                        applicationId,
                        "REJECTED",
                        "Interview scorecard recommendation: " + recommendation
                );
            }
        } catch (Exception e) {
            log.warn("[ScorecardService] Failed to auto-move application stage after scorecard submission: {}", e.getMessage());
        }
    }

    private Double calculateAverageScore(ScorecardRequestDto dto) {

        double total =
                dto.getTechnicalScore()
                        + dto.getCommunicationScore()
                        + dto.getProblemSolvingScore()
                        + dto.getCultureFitScore();

        return total / 4;
    }

    private ScorecardResponseDto toResponseDto(Scorecard scorecard) {

        return ScorecardResponseDto.builder()
                .scorecardId(scorecard.getId())
                .interviewId(scorecard.getInterview().getInterviewId())
                .applicationId(scorecard.getApplicationId())
                .interviewerId(scorecard.getInterviewerId())
                .technicalScore(scorecard.getTechnicalScore())
                .communicationScore(scorecard.getCommunicationScore())
                .problemSolvingScore(scorecard.getProblemSolvingScore())
                .cultureFitScore(scorecard.getCultureFitScore())
                .averageScore(scorecard.getAverageScore())
                .recommendation(scorecard.getRecommendation())
                .overallFeedback(scorecard.getOverallFeedback())
                .submittedAt(scorecard.getSubmittedAt())
                .build();
    }

    private ScorecardSummaryDto toSummaryDto(Scorecard scorecard) {

        return ScorecardSummaryDto.builder()
                .scorecardId(scorecard.getId())
                .interviewId(scorecard.getInterview().getInterviewId())
                .applicationId(scorecard.getApplicationId())
                .interviewerId(scorecard.getInterviewerId())
                .averageScore(scorecard.getAverageScore())
                .recommendation(scorecard.getRecommendation())
                .submittedAt(scorecard.getSubmittedAt())
                .build();
    }
}