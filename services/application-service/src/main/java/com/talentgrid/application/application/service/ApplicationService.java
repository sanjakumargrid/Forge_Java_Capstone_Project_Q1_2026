package com.talentgrid.application.application.service;

import com.talentgrid.application.application.dto.ApplicationDto;
import com.talentgrid.application.application.dto.DemandDto;
import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;
import com.talentgrid.application.application.dto.candidate.SkillDetailDto;
import com.talentgrid.application.application.dto.request.StageMoveRequest;
import com.talentgrid.application.application.entity.Application;
import com.talentgrid.application.application.enums.Stage;
import com.talentgrid.application.application.mapper.ApplicationMapper;
import com.talentgrid.application.application.repository.ApplicationRepository;
import com.talentgrid.application.client.CandidateClient;
import com.talentgrid.application.client.DemandClient;
import com.talentgrid.application.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateClient candidateClient;
    private final DemandClient demandClient;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            CandidateClient candidateClient,
            DemandClient demandClient
    ) {
        this.applicationRepository = applicationRepository;
        this.candidateClient = candidateClient;
        this.demandClient = demandClient;
    }

    @Transactional
    public ApplicationDto createApplication(ApplicationDto applicationDto) {

        if (applicationDto == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Application details are required");
        }

        validateCreateRequest(applicationDto);

        ExternalCandidateDto candidateDto =
                candidateClient.getCandidate(applicationDto.getCandidateId());

        if (candidateDto == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Candidate not found with id: " + applicationDto.getCandidateId()
            );
        }

        DemandDto demandDto =
                demandClient.getDemand(applicationDto.getDemandId());

        if (demandDto == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Demand not found with id: " + applicationDto.getDemandId()
            );
        }

        boolean alreadyApplied =
                applicationRepository.existsByCandidateIdAndDemandId(
                        applicationDto.getCandidateId(),
                        applicationDto.getDemandId()
                );

        if (alreadyApplied) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Candidate already applied for this demand"
            );
        }

        calculateSkillMatching(applicationDto, candidateDto, demandDto);

        applicationDto.setCurrentStage(Stage.APPLIED);
        applicationDto.setBlockedFromReapply(false);

        Application application =
                ApplicationMapper.dtoToApplicationEntity(applicationDto);

        Application saved = applicationRepository.save(application);

        return ApplicationMapper.applicationEntityToDto(saved);
    }

    public Page<ApplicationDto> getApplications(
            Long demandId,
            String stage,
            int page,
            int size
    ) {

        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications;

        if (demandId != null && stage != null && !stage.isBlank()) {
            applications = applicationRepository.findByDemandIdAndCurrentStage(
                    demandId,
                    parseStage(stage),
                    pageable
            );
        } else if (demandId != null) {
            applications = applicationRepository.findByDemandId(demandId, pageable);
        } else if (stage != null && !stage.isBlank()) {
            applications = applicationRepository.findByCurrentStage(
                    parseStage(stage),
                    pageable
            );
        } else {
            applications = applicationRepository.findAll(pageable);
        }

        return applications.map(ApplicationMapper::applicationEntityToDto);
    }

    public ApplicationDto getApplicationById(Long applicationId) {
        return ApplicationMapper.applicationEntityToDto(
                getApplicationEntity(applicationId)
        );
    }

    @Transactional
    public ApplicationDto moveStage(
            Long applicationId,
            StageMoveRequest request
    ) {

        if (request == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Request body is required"
            );
        }

        Application application = getApplicationEntity(applicationId);

        Stage targetStage = parseStage(request.getTargetStage());
        String reason = request.getReason();

        validateStageMovement(
                application.getCurrentStage(),
                targetStage,
                reason
        );

        application.setCurrentStage(targetStage);
        application.setStageMoveReason(reason);

        setStageTimestamp(application, targetStage);

        if (targetStage == Stage.REJECTED) {
            application.setRejectionReason(reason);
            application.setBlockedFromReapply(true);
        }

        Application updated = applicationRepository.save(application);

        return ApplicationMapper.applicationEntityToDto(updated);
    }

    public List<String> getTimeline(Long applicationId) {

        Application application = getApplicationEntity(applicationId);
        List<String> timeline = new ArrayList<>();

        if (application.getAppliedAt() != null) {
            timeline.add("APPLIED : " + application.getAppliedAt());
        }

        if (application.getScreeningAt() != null) {
            timeline.add("SCREENING : " + application.getScreeningAt());
        }

        if (application.getTechnicalAt() != null) {
            timeline.add("TECHNICAL : " + application.getTechnicalAt());
        }

        if (application.getInterviewAt() != null) {
            timeline.add("INTERVIEW : " + application.getInterviewAt());
        }

        if (application.getFinalRoundAt() != null) {
            timeline.add("FINAL_ROUND : " + application.getFinalRoundAt());
        }

        if (application.getOfferAt() != null) {
            timeline.add("OFFER : " + application.getOfferAt());
        }

        if (application.getHiredAt() != null) {
            timeline.add("HIRED : " + application.getHiredAt());
        }

        if (application.getRejectedAt() != null) {
            timeline.add("REJECTED : " + application.getRejectedAt());
        }

        return timeline;
    }

    public Page<ApplicationDto> searchApplications(
            Integer minScore,
            int page,
            int size
    ) {

        validatePageRequest(page, size);

        if (minScore != null && (minScore < 0 || minScore > 100)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum score must be between 0 and 100"
            );
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Application> applications =
                applicationRepository.findByAiScoreGreaterThanEqual(
                        minScore == null ? 0 : minScore,
                        pageable
                );

        return applications.map(ApplicationMapper::applicationEntityToDto);
    }

    private Application getApplicationEntity(Long applicationId) {

        if (applicationId == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Application id is required"
            );
        }

        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Application not found with id: " + applicationId
                ));
    }

    private void validateCreateRequest(ApplicationDto applicationDto) {

        List<String> errors = new ArrayList<>();

        if (applicationDto.getCandidateId() == null) {
            errors.add("Candidate ID is required");
        }

        if (applicationDto.getDemandId() == null) {
            errors.add("Demand ID is required");
        }

        if (applicationDto.getSource() == null) {
            errors.add("Source is required");
        }

        if (applicationDto.getResumeFilePath() == null ||
                applicationDto.getResumeFilePath().isBlank()) {
            errors.add("Resume file path is required");
        }

        if (applicationDto.getResumeOriginalFilename() == null ||
                applicationDto.getResumeOriginalFilename().isBlank()) {
            errors.add("Original resume filename is required");
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, errors);
        }
    }

    private void validatePageRequest(int page, int size) {

        if (page < 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative"
            );
        }

        if (size <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be greater than 0"
            );
        }
    }

    private void validateStageMovement(
            Stage currentStage,
            Stage targetStage,
            String reason
    ) {

        if (currentStage == Stage.HIRED || currentStage == Stage.REJECTED) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot move application from terminal stage: " + currentStage
            );
        }

        if (targetStage == Stage.APPLIED) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot move back to APPLIED stage"
            );
        }

        if (targetStage == Stage.REJECTED) {
            if (reason == null || reason.isBlank()) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Rejection reason is required"
                );
            }
            return;
        }

        Stage expectedNextStage = getNextStage(currentStage);

        if (targetStage != expectedNextStage) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid stage movement. Current stage is "
                            + currentStage
                            + ". Allowed next stage is "
                            + expectedNextStage
                            + " or REJECTED"
            );
        }
    }

    private Stage getNextStage(Stage currentStage) {

        return switch (currentStage) {
            case APPLIED -> Stage.SCREENING;
            case SCREENING -> Stage.TECHNICAL;
            case TECHNICAL -> Stage.INTERVIEW;
            case INTERVIEW -> Stage.FINAL_ROUND;
            case FINAL_ROUND -> Stage.OFFERED;
            case OFFERED -> Stage.HIRED;
            case HIRED, REJECTED -> null;
        };
    }

    private void setStageTimestamp(
            Application application,
            Stage stage
    ) {

        LocalDateTime now = LocalDateTime.now();

        switch (stage) {
            case SCREENING -> application.setScreeningAt(now);
            case TECHNICAL -> application.setTechnicalAt(now);
            case INTERVIEW -> application.setInterviewAt(now);
            case FINAL_ROUND -> application.setFinalRoundAt(now);
            case OFFERED -> application.setOfferAt(now);
            case HIRED -> application.setHiredAt(now);
            case REJECTED -> application.setRejectedAt(now);
            case APPLIED -> {
                // APPLIED is created automatically using appliedAt.
            }
        }
    }

    private void calculateSkillMatching(
            ApplicationDto applicationDto,
            ExternalCandidateDto candidateDto,
            DemandDto demandDto
    ) {

        List<String> requiredSkills = normalizeSkills(demandDto.getSkills());

        List<String> candidateSkills =
                candidateDto.getSkills() == null
                        ? List.of()
                        : normalizeSkills(
                        candidateDto.getSkills()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(SkillDetailDto::getSkillName)
                        .collect(Collectors.toList())
                );

        List<String> matchedSkills =
                candidateSkills.stream()
                        .filter(candidateSkill ->
                                requiredSkills.stream()
                                        .anyMatch(requiredSkill ->
                                                requiredSkill.equalsIgnoreCase(candidateSkill)
                                        )
                        )
                        .collect(Collectors.toList());

        List<String> missingSkills =
                requiredSkills.stream()
                        .filter(requiredSkill ->
                                candidateSkills.stream()
                                        .noneMatch(candidateSkill ->
                                                candidateSkill.equalsIgnoreCase(requiredSkill)
                                        )
                        )
                        .collect(Collectors.toList());

        List<String> otherSkills =
                candidateSkills.stream()
                        .filter(candidateSkill ->
                                requiredSkills.stream()
                                        .noneMatch(requiredSkill ->
                                                requiredSkill.equalsIgnoreCase(candidateSkill)
                                        )
                        )
                        .collect(Collectors.toList());

        applicationDto.setMatchedSkills(matchedSkills);
        applicationDto.setMissingSkills(missingSkills);
        applicationDto.setOtherSkills(otherSkills);
        applicationDto.setAiScore(calculateAiScore(requiredSkills, matchedSkills));
        applicationDto.setAiRationale(
                buildAiRationale(matchedSkills, missingSkills, otherSkills)
        );
    }

    private List<String> normalizeSkills(List<String> skills) {

        if (skills == null) {
            return List.of();
        }

        return skills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .map(skill -> skill.toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
    }

    private Integer calculateAiScore(
            List<String> requiredSkills,
            List<String> matchedSkills
    ) {

        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 0;
        }

        double score =
                ((double) matchedSkills.size() / requiredSkills.size()) * 100;

        return (int) Math.round(score);
    }

    private String buildAiRationale(
            List<String> matchedSkills,
            List<String> missingSkills,
            List<String> otherSkills
    ) {

        return "Candidate matched "
                + matchedSkills.size()
                + " required skills. Matched skills: "
                + matchedSkills
                + ". Missing skills: "
                + missingSkills
                + ". Other skills: "
                + otherSkills
                + ".";
    }

    private Stage parseStage(String stage) {

        if (stage == null || stage.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Stage is required"
            );
        }

        try {
            return Stage.valueOf(stage.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid stage: " + stage
            );
        }
    }
}