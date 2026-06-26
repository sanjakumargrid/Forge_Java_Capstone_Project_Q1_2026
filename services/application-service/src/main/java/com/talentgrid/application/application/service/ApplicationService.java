package com.talentgrid.application.application.service;

import com.talentgrid.application.application.dto.ApplicationDto;
import com.talentgrid.application.application.dto.JobPostingDto;
import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;
import com.talentgrid.application.application.dto.candidate.SkillDetailDto;
import com.talentgrid.application.application.dto.request.ApplicationCreateRequest;
import com.talentgrid.application.application.dto.request.AtsEvaluationPayload;
import com.talentgrid.application.application.dto.request.BulkJobPostingReassignRequest;
import com.talentgrid.application.application.dto.request.BulkRejectRequest;
import com.talentgrid.application.application.dto.request.BulkStageMoveRequest;
import com.talentgrid.application.application.dto.request.StageMoveRequest;
import com.talentgrid.application.application.entity.Application;
import com.talentgrid.application.application.enums.Stage;
import com.talentgrid.application.application.mapper.ApplicationMapper;
import com.talentgrid.application.application.repository.ApplicationRepository;
import com.talentgrid.application.client.CandidateClient;
import com.talentgrid.application.client.JobPostingClient;
import com.talentgrid.application.exception.BusinessException;
import com.talentgrid.application.kafka.producer.ApplicationEventProducer;
import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateClient candidateClient;
    private final JobPostingClient jobPostingClient;
    private final AuditLogClient auditLogClient;
    private final ApplicationEventProducer applicationEventProducer;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            CandidateClient candidateClient,
            JobPostingClient jobPostingClient,
            AuditLogClient auditLogClient,
            ApplicationEventProducer applicationEventProducer
    ) {
        this.applicationRepository = applicationRepository;
        this.candidateClient = candidateClient;
        this.jobPostingClient = jobPostingClient;
        this.auditLogClient = auditLogClient;
        this.applicationEventProducer = applicationEventProducer;
    }

    @Transactional
    public ApplicationDto createApplication(ApplicationCreateRequest request) {

        if (request == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Application details are required"
            );
        }

        if (request.getCandidateId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Candidate id is required"
            );
        }

        if (request.getJobPostingId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Job posting id is required"
            );
        }

        if (request.getDemandId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Demand id is required"
            );
        }

        ExternalCandidateDto candidateDto =
                candidateClient.getCandidateById(request.getCandidateId());

        if (candidateDto == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Candidate not found with id: " + request.getCandidateId()
            );
        }

        JobPostingDto jobPostingDto =
                jobPostingClient.getJobPosting(request.getJobPostingId());

        if (jobPostingDto == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Job posting not found with id: " + request.getJobPostingId()
            );
        }

        /*
         * No job-service change needed.
         * We are using existing API:
         * GET /api/job-postings/{jobPostingId}
         *
         * If job-service response has demandId, validate it.
         * If job-service response does not have demandId, we trust request demandId.
         */
        if (jobPostingDto.getDemandId() != null
                && !request.getDemandId().equals(jobPostingDto.getDemandId())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Given demandId does not match with job posting demandId"
            );
        }

        Application existingApplication =
                applicationRepository
                        .findFirstByCandidateIdAndJobPostingIdAndDemandId(
                                request.getCandidateId(),
                                request.getJobPostingId(),
                                request.getDemandId()
                        )
                        .orElse(null);

        if (existingApplication != null) {
            ApplicationDto existingDto =
                    toDtoWithCandidateAndJobPosting(
                            existingApplication,
                            candidateDto,
                            jobPostingDto
                    );

            existingDto.setApplicationAlreadyExists(true);
            existingDto.setMessage(
                    "Application already exists for this candidate, job posting, and demand"
            );

            return existingDto;
        }

        ApplicationDto applicationDto = new ApplicationDto();
        applicationDto.setCandidateId(request.getCandidateId());
        applicationDto.setJobPostingId(request.getJobPostingId());
        applicationDto.setDemandId(request.getDemandId());
        applicationDto.setSource(request.getSource());
        applicationDto.setResumeFilePath(request.getResumeFilePath());
        applicationDto.setResumeOriginalFilename(request.getResumeOriginalFilename());
        applicationDto.setFreeNotes(request.getFreeNotes());
        applicationDto.setReferralCode(request.getReferralCode());

        calculateSkillMatching(applicationDto, candidateDto, jobPostingDto);

        if (applicationDto.getAiScore() < 50) {
            applicationDto.setCurrentStage(Stage.REJECTED);
            applicationDto.setRejectionReason(
                    "ATS score is below the minimum threshold for this role."
            );
            applicationDto.setBlockedFromReapply(true);
        } else {
            applicationDto.setCurrentStage(Stage.SCREENING);
            applicationDto.setBlockedFromReapply(false);
        }

        Application application =
                ApplicationMapper.dtoToApplicationEntity(applicationDto);

        Application saved =
                applicationRepository.saveAndFlush(application);

        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("APPLICATION")
                        .entityId(saved.getId())
                        .action(AuditAction.CREATE)
                        .afterState(Map.of(
                                "candidateId", saved.getCandidateId(),
                                "jobPostingId", saved.getJobPostingId(),
                                "demandId", saved.getDemandId(),
                                "stage", saved.getCurrentStage().name()
                        ))
                        .serviceName("application-service")
                        .endpoint("/api/v1/applications")
                        .build()
        );

        if (saved.getCurrentStage() == Stage.REJECTED) {
            applicationEventProducer.publishRejected(saved);
        } else {
            applicationEventProducer.publishApplied(saved);
        }

        ApplicationDto response =
                toDtoWithCandidateAndJobPosting(
                        saved,
                        candidateDto,
                        jobPostingDto
                );

        response.setApplicationAlreadyExists(false);
        response.setMessage("Application created successfully");

        return response;
    }

    public Page<ApplicationDto> getApplications(
            Long jobPostingId,
            String stage,
            int page,
            int size
    ) {
        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications;

        if (jobPostingId != null && stage != null && !stage.isBlank()) {
            applications =
                    applicationRepository.findByJobPostingIdAndCurrentStage(
                            jobPostingId,
                            parseStage(stage),
                            pageable
                    );
        } else if (jobPostingId != null) {
            applications =
                    applicationRepository.findByJobPostingId(
                            jobPostingId,
                            pageable
                    );
        } else if (stage != null && !stage.isBlank()) {
            applications =
                    applicationRepository.findByCurrentStage(
                            parseStage(stage),
                            pageable
                    );
        } else {
            applications = applicationRepository.findAll(pageable);
        }

        return applications.map(this::toDtoWithCandidate);
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

        return applicationRepository
                .findByAiScoreGreaterThanEqual(
                        minScore == null ? 0 : minScore,
                        pageable
                )
                .map(this::toDtoWithCandidate);
    }

    public ApplicationDto getApplicationById(Long applicationId) {
        return toDtoWithCandidate(getApplicationEntity(applicationId));
    }

    public List<ApplicationDto> getApplicationsByCandidateAndDemand(
            Long candidateId,
            Long demandId
    ) {
        if (candidateId == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Candidate id is required"
            );
        }

        if (demandId == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Demand id is required"
            );
        }

        List<Application> applications =
                applicationRepository.findByCandidateIdAndDemandId(
                        candidateId,
                        demandId
                );

        return applications.stream()
                .map(this::toDtoWithCandidate)
                .collect(Collectors.toList());
    }

    public List<ExternalCandidateDto> getCandidatesByDemand(Long demandId) {

        if (demandId == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Demand id is required"
            );
        }

        List<Application> applications =
                applicationRepository.findByDemandId(demandId);

        return applications.stream()
                .map(Application::getCandidateId)
                .filter(Objects::nonNull)
                .distinct()
                .map(candidateClient::getCandidateById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

        Stage previousStage = application.getCurrentStage();

        application.setCurrentStage(targetStage);
        application.setStageMoveReason(reason);
        setStageTimestamp(application, targetStage);

        if (targetStage == Stage.REJECTED) {
            application.setRejectionReason(reason);
            application.setBlockedFromReapply(true);
        }

        Application updated = applicationRepository.save(application);

        if (targetStage == Stage.HIRED) {
            List<Application> otherActiveApplications =
                    applicationRepository.findByJobPostingIdAndCurrentStageNotIn(
                            updated.getJobPostingId(),
                            List.of(Stage.HIRED, Stage.REJECTED)
                    );

            for (Application otherApp : otherActiveApplications) {
                otherApp.setCurrentStage(Stage.REJECTED);
                otherApp.setRejectionReason("Position has been filled.");
                otherApp.setRejectedAt(LocalDateTime.now());
                otherApp.setBlockedFromReapply(false);

                applicationRepository.save(otherApp);

                applicationEventProducer.publishRejected(otherApp);
            }

            try {
                JobPostingDto jobPosting =
                        jobPostingClient.getJobPosting(updated.getJobPostingId());

                String recruiterEmail =
                        com.talentgrid.application.util.SecurityUtils
                                .getCurrentUserEmail();

                if (jobPosting != null && recruiterEmail != null) {
                    applicationEventProducer.publishJobPostingClosedNotification(
                            jobPosting.getJobPostingId(),
                            jobPosting.getTitle() != null
                                    ? jobPosting.getTitle()
                                    : "Job Posting " + jobPosting.getJobPostingId(),
                            recruiterEmail
                    );
                }
            } catch (Exception ignored) {
                // Do not block stage movement if notification fails.
            }
        }

        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("APPLICATION")
                        .entityId(updated.getId())
                        .action(AuditAction.STATUS_CHANGE)
                        .beforeState(Map.of("stage", previousStage.name()))
                        .afterState(Map.of(
                                "stage",
                                updated.getCurrentStage().name()
                        ))
                        .serviceName("application-service")
                        .endpoint("/api/applications/" + applicationId + "/stage")
                        .build()
        );

        publishStageEvent(updated, targetStage);

        return toDtoWithCandidate(updated);
    }

    @Transactional
    public void updateAiEvaluation(
            Long applicationId,
            AtsEvaluationPayload payload
    ) {
        Application application = getApplicationEntity(applicationId);

        application.setAiScore(payload.getAiScore());
        application.setMatchedSkills(payload.getMatchedSkills());
        application.setMissingSkills(payload.getMissingSkills());
        application.setOtherSkills(payload.getOtherSkills());

        applicationRepository.save(application);
    }

    @Transactional
    public List<ApplicationDto> bulkMoveStage(BulkStageMoveRequest request) {

        if (request == null
                || request.getApplicationIds() == null
                || request.getApplicationIds().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "At least one application ID is required"
            );
        }

        if (request.getTargetStage() == null
                || request.getTargetStage().isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Target stage is required"
            );
        }

        List<ApplicationDto> result = new ArrayList<>();

        for (Long applicationId : request.getApplicationIds()) {
            StageMoveRequest stageMoveRequest = new StageMoveRequest();
            stageMoveRequest.setTargetStage(request.getTargetStage());
            stageMoveRequest.setReason(request.getReason());

            result.add(moveStage(applicationId, stageMoveRequest));
        }

        return result;
    }

    @Transactional
    public List<ApplicationDto> bulkReject(BulkRejectRequest request) {

        if (request == null
                || request.getApplicationIds() == null
                || request.getApplicationIds().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "At least one application ID is required"
            );
        }

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Rejection reason is required"
            );
        }

        List<ApplicationDto> result = new ArrayList<>();

        for (Long applicationId : request.getApplicationIds()) {
            StageMoveRequest stageMoveRequest = new StageMoveRequest();
            stageMoveRequest.setTargetStage(Stage.REJECTED.name());
            stageMoveRequest.setReason(request.getReason());

            result.add(moveStage(applicationId, stageMoveRequest));
        }

        return result;
    }

    @Transactional
    public List<ApplicationDto> bulkReassignJobPosting(
            BulkJobPostingReassignRequest request
    ) {

        if (request == null
                || request.getApplicationIds() == null
                || request.getApplicationIds().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "At least one application ID is required"
            );
        }

        if (request.getTargetJobPostingId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Target job posting ID is required"
            );
        }

        JobPostingDto targetJobPosting =
                jobPostingClient.getJobPosting(request.getTargetJobPostingId());

        if (targetJobPosting == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Job posting not found with id: "
                            + request.getTargetJobPostingId()
            );
        }

        List<Application> applications =
                applicationRepository.findAllByIdIn(
                        request.getApplicationIds()
                );

        List<ApplicationDto> results = new ArrayList<>();

        for (Application app : applications) {
            boolean alreadyAppliedToTarget =
                    applicationRepository.existsByCandidateIdAndJobPostingId(
                            app.getCandidateId(),
                            request.getTargetJobPostingId()
                    );

            if (alreadyAppliedToTarget) {
                continue;
            }

            Long oldJobPostingId = app.getJobPostingId();

            app.setJobPostingId(request.getTargetJobPostingId());

            Application saved = applicationRepository.save(app);

            auditLogClient.logAction(
                    AuditLogPayload.builder()
                            .entityType("APPLICATION")
                            .entityId(saved.getId())
                            .action(AuditAction.UPDATE)
                            .beforeState(Map.of(
                                    "jobPostingId",
                                    oldJobPostingId
                            ))
                            .afterState(Map.of(
                                    "jobPostingId",
                                    request.getTargetJobPostingId()
                            ))
                            .serviceName("application-service")
                            .endpoint(
                                    "/api/applications/bulk/reassign-job-posting"
                            )
                            .build()
            );

            results.add(toDtoWithCandidate(saved));
        }

        return results;
    }

    public String exportToCsv(Long jobPostingId, String stage) {

        if (jobPostingId == null && (stage == null || stage.isBlank())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "At least one filter jobPostingId or stage is required for CSV export"
            );
        }

        List<Application> applications;

        if (jobPostingId != null && stage != null && !stage.isBlank()) {
            applications =
                    applicationRepository
                            .findByJobPostingIdAndCurrentStage(
                                    jobPostingId,
                                    parseStage(stage),
                                    Pageable.unpaged()
                            )
                            .getContent();

        } else if (jobPostingId != null) {
            applications =
                    applicationRepository
                            .findByJobPostingId(
                                    jobPostingId,
                                    Pageable.unpaged()
                            )
                            .getContent();

        } else {
            applications =
                    applicationRepository
                            .findByCurrentStage(
                                    parseStage(stage),
                                    Pageable.unpaged()
                            )
                            .getContent();
        }

        StringBuilder csv = new StringBuilder();

        csv.append(
                "\"Application ID\",\"Candidate ID\",\"Job Posting ID\",\"Demand ID\",\"Source\",\"Current Stage\",\"Applied At\",\"AI Score\"\n"
        );

        for (Application app : applications) {
            csv.append(csvSafe(app.getId())).append(",")
                    .append(csvSafe(app.getCandidateId())).append(",")
                    .append(csvSafe(app.getJobPostingId())).append(",")
                    .append(csvSafe(app.getDemandId())).append(",")
                    .append(csvSafe(app.getSource())).append(",")
                    .append(csvSafe(app.getCurrentStage())).append(",")
                    .append(csvSafe(app.getAppliedAt())).append(",")
                    .append(csvSafe(app.getAiScore())).append("\n");
        }

        return csv.toString();
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
            timeline.add("OFFERED : " + application.getOfferAt());
        }

        if (application.getHiredAt() != null) {
            timeline.add("HIRED : " + application.getHiredAt());
        }

        if (application.getRejectedAt() != null) {
            timeline.add("REJECTED : " + application.getRejectedAt());
        }

        return timeline;
    }

    private void calculateSkillMatching(
            ApplicationDto applicationDto,
            ExternalCandidateDto candidateDto,
            JobPostingDto jobPostingDto
    ) {
        List<String> requiredSkills =
                jobPostingDto.getSkills() == null
                        ? List.of()
                        : jobPostingDto.getSkills()
                          .stream()
                          .filter(Objects::nonNull)
                          .map(String::trim)
                          .filter(skill -> !skill.isBlank())
                          .distinct()
                          .collect(Collectors.toList());

        List<String> candidateSkills =
                candidateDto.getSkills() == null
                        ? List.of()
                        : candidateDto.getSkills()
                          .stream()
                          .filter(Objects::nonNull)
                          .map(SkillDetailDto::getSkillName)
                          .filter(Objects::nonNull)
                          .map(String::trim)
                          .filter(skill -> !skill.isBlank())
                          .distinct()
                          .collect(Collectors.toList());

        List<String> normalizedRequiredSkills =
                requiredSkills.stream()
                        .map(this::normalizeSkill)
                        .collect(Collectors.toList());

        List<String> normalizedCandidateSkills =
                candidateSkills.stream()
                        .map(this::normalizeSkill)
                        .collect(Collectors.toList());

        List<String> matchedSkills =
                candidateSkills.stream()
                        .filter(candidateSkill ->
                                normalizedRequiredSkills.contains(
                                        normalizeSkill(candidateSkill)
                                )
                        )
                        .collect(Collectors.toList());

        List<String> missingSkills =
                requiredSkills.stream()
                        .filter(requiredSkill ->
                                !normalizedCandidateSkills.contains(
                                        normalizeSkill(requiredSkill)
                                )
                        )
                        .collect(Collectors.toList());

        List<String> otherSkills =
                candidateSkills.stream()
                        .filter(candidateSkill ->
                                !normalizedRequiredSkills.contains(
                                        normalizeSkill(candidateSkill)
                                )
                        )
                        .collect(Collectors.toList());

        applicationDto.setMatchedSkills(matchedSkills);
        applicationDto.setMissingSkills(missingSkills);
        applicationDto.setOtherSkills(otherSkills);
        applicationDto.setAiScore(
                calculateAiScore(requiredSkills, matchedSkills)
        );
        applicationDto.setAiRationale(
                buildAiRationale(
                        matchedSkills,
                        missingSkills,
                        otherSkills
                )
        );
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
        String rationale =
                "Candidate matched "
                        + matchedSkills.size()
                        + " required skills. Matched: "
                        + matchedSkills
                        + ". Missing: "
                        + missingSkills
                        + ". Other: "
                        + otherSkills
                        + ".";

        if (rationale.length() > 300) {
            return rationale.substring(0, 297) + "...";
        }

        return rationale;
    }

    private String normalizeSkill(String skill) {
        if (skill == null) {
            return "";
        }

        return skill
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private ApplicationDto toDtoWithCandidate(Application application) {

        ApplicationDto dto =
                ApplicationMapper.applicationEntityToDto(application);

        if (dto == null) {
            return null;
        }

        if (dto.getCandidateId() != null) {
            dto.setCandidate(
                    candidateClient.getCandidateById(dto.getCandidateId())
            );
        }

        if (dto.getJobPostingId() != null) {
            dto.setJobPosting(
                    jobPostingClient.getJobPosting(dto.getJobPostingId())
            );
        }

        return dto;
    }

    private ApplicationDto toDtoWithCandidateAndJobPosting(
            Application application,
            ExternalCandidateDto candidate,
            JobPostingDto jobPosting
    ) {
        ApplicationDto dto =
                ApplicationMapper.applicationEntityToDto(application);

        if (dto != null) {
            dto.setCandidate(candidate);
            dto.setJobPosting(jobPosting);
        }

        return dto;
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
                    "Cannot move application from terminal stage: "
                            + currentStage
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
            }
        }
    }

    private void publishStageEvent(
            Application application,
            Stage stage
    ) {
        switch (stage) {
            case SCREENING -> applicationEventProducer.publishScreening(application);
            case TECHNICAL -> applicationEventProducer.publishTechnical(application);
            case INTERVIEW -> applicationEventProducer.publishInterview(application);
            case FINAL_ROUND -> applicationEventProducer.publishFinalRound(application);
            case OFFERED -> applicationEventProducer.publishOffered(application);
            case HIRED -> applicationEventProducer.publishHired(application);
            case REJECTED -> applicationEventProducer.publishRejected(application);
            case APPLIED -> applicationEventProducer.publishApplied(application);
        }
    }

    private String csvSafe(Object value) {
        if (value == null) {
            return "\"\"";
        }

        String s = value.toString().replace("\"", "\"\"");

        return "\"" + s + "\"";
    }
}