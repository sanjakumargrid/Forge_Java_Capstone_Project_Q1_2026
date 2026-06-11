package com.talentgrid.application.application.service;

import com.talentgrid.application.application.dto.ApplicationDto;
import com.talentgrid.application.application.entity.Application;
import com.talentgrid.application.application.enums.Stage;
import com.talentgrid.application.application.mapper.ApplicationMapper;
import com.talentgrid.application.application.repository.ApplicationRepository;
import com.talentgrid.application.client.CandidateClient;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

  private final ApplicationRepository applicationRepository;
  CandidateClient candidateClient;
  //DemandClient demandClient;

  public ApplicationService(
          ApplicationRepository applicationRepository,
          CandidateClient candidateClient;
          //DemandClient demandClient;
  ) {
    this.applicationRepository = applicationRepository;
    this.candidateClient = candidateClient;
    //this.demandClient = demandClient;
  }

  @Transactional
  public ApplicationDto createApplication(ApplicationDto dto) {

    if (dto == null) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Application details are required"
      );
    }

    Demand demand = getOrCreateDemand(dto.getDemand());

    boolean alreadyApplied =
            applicationRepository.existsByCandidateEmailAndDemandDemandId(
                    dto.getCandidateEmail(),
                    demand.getDemandId()
            );

    if (alreadyApplied) {
      throw new BusinessException(
              HttpStatus.CONFLICT,
              "Candidate already applied for this demand"
      );
    }

    calculateSkillMatching(dto, demand);

    if (dto.getCurrentStage() == null) {
      dto.setCurrentStage(Stage.APPLIED);
    }

    if (dto.getBlockedFromReapply() == null) {
      dto.setBlockedFromReapply(false);
    }

    Application application =
            ApplicationMapper.dtoToApplicationEntity(dto, demand);

    Application saved =
            applicationRepository.save(application);

    return ApplicationMapper.applicationEntityToDto(saved);
  }


  public Page<ApplicationDto> getApplications(
          Long demandId,
          String stage,
          int page,
          int size
  ) {

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

    Pageable pageable = PageRequest.of(page, size);

    Page<Application> applications;

    if (demandId != null && stage != null && !stage.isBlank()) {
      applications =
              applicationRepository.findByDemandIdAndCurrentStage(
                      demandId,
                      parseStage(stage),
                      pageable
              );
    } else if (demandId != null) {
      applications =
              applicationRepository.findByDemandDemandId(
                      demandId,
                      pageable
              );
    } else if (stage != null && !stage.isBlank()) {
      applications =
              applicationRepository.findByCurrentStage(
                      parseStage(stage),
                      pageable
              );
    } else {
      applications =
              applicationRepository.findAll(pageable);
    }

    return applications.map(ApplicationMapper::applicationEntityToDto);
  }

  public ApplicationDto getApplicationById(Long applicationId) {

    if (applicationId == null) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Application id is required"
      );
    }

    Application application =
            applicationRepository.findById(applicationId)
                    .orElseThrow(() ->
                            new BusinessException(
                                    HttpStatus.NOT_FOUND,
                                    "Application not found with id: " + applicationId
                            )
                    );

    return ApplicationMapper.applicationEntityToDto(application);
  }

  @Transactional
  public ApplicationDto moveStage(
          Long applicationId,
          java.util.Map<String, String> request
  ) {

    if (applicationId == null) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Application id is required"
      );
    }

    if (request == null) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Request body is required"
      );
    }

    Application application =
            applicationRepository.findById(applicationId)
                    .orElseThrow(() ->
                            new BusinessException(
                                    HttpStatus.NOT_FOUND,
                                    "Application not found with id: " + applicationId
                            )
                    );

    String targetStage = request.get("targetStage");
    String reason = request.get("reason");

    if (targetStage == null || targetStage.isBlank()) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Target stage is required"
      );
    }

    Stage stage = parseStage(targetStage);

    application.setCurrentStage(stage);
    application.setStageMoveReason(reason);
    application.updateStageTimestamp();

    if (stage == Stage.REJECTED) {

      if (reason == null || reason.isBlank()) {
        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Rejection reason is required"
        );
      }

      application.setRejectionReason(reason);
      application.setBlockedFromReapply(true);
      application.setReapplyAllowedAfter(
              LocalDateTime.now().plusMonths(6)
      );
    }

    Application updated =
            applicationRepository.save(application);

    return ApplicationMapper.applicationEntityToDto(updated);
  }

  @Transactional
  public ApplicationDto withdrawApplication(
          Long applicationId,
          java.util.Map<String, String> request
  ) {

    if (applicationId == null) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Application id is required"
      );
    }

    if (request == null) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Request body is required"
      );
    }

    Application application =
            applicationRepository.findById(applicationId)
                    .orElseThrow(() ->
                            new BusinessException(
                                    HttpStatus.NOT_FOUND,
                                    "Application not found with id: " + applicationId
                            )
                    );

    String reason = request.get("reason");

    if (reason == null || reason.isBlank()) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Withdraw reason is required"
      );
    }

    application.setCurrentStage(Stage.WITHDRAWN);
    application.setStageMoveReason(reason);
    application.updateStageTimestamp();

    Application updated =
            applicationRepository.save(application);

    return ApplicationMapper.applicationEntityToDto(updated);
  }

  public List<String> getTimeline(Long applicationId) {

    if (applicationId == null) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Application id is required"
      );
    }

    Application application =
            applicationRepository.findById(applicationId)
                    .orElseThrow(() ->
                            new BusinessException(
                                    HttpStatus.NOT_FOUND,
                                    "Application not found with id: " + applicationId
                            )
                    );

    List<String> timeline = new ArrayList<>();

    if (application.getRejectedAt() != null) {
      timeline.add(
              "REJECTED : " + application.getRejectedAt()
      );
    }

    if (application.getWithdrawnAt() != null) {
      timeline.add(
              "WITHDRAWN : " + application.getWithdrawnAt()
      );
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
      timeline.add(application.getCurrentStage() + " : " + application.getRejectedAt());
    }

    return timeline;
  }

  public Page<ApplicationDto> searchApplications(
          Integer minScore,
          int page,
          int size
  ) {

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

  private Demand getOrCreateDemand(DemandDto demandDto) {

    if (demandDto == null) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Demand details are required"
      );
    }

    if (demandDto.getDemandId() != null) {
      return demandRepository.findById(demandDto.getDemandId())
              .orElseGet(() -> saveNewDemand(demandDto));
    }

    if (demandDto.getDemandCode() != null && !demandDto.getDemandCode().isBlank()) {
      Optional<Demand> existingDemand =
              demandRepository.findByDemandCode(demandDto.getDemandCode());

      if (existingDemand.isPresent()) {
        return existingDemand.get();
      }
    }

    return saveNewDemand(demandDto);
  }

  private Demand saveNewDemand(DemandDto demandDto) {

    validateDemandDetails(demandDto);

    if (demandDto.getStatus() == null) {
      demandDto.setStatus(DemandStatus.OPEN);
    }

    Demand demand =
            ApplicationMapper.dtoToDemandEntity(demandDto);

    return demandRepository.save(demand);
  }

  private void validateDemandDetails(DemandDto demandDto) {

    List<String> errors = new ArrayList<>();

    if (demandDto.getDemandCode() == null || demandDto.getDemandCode().isBlank()) {
      errors.add("Demand code is required");
    }

    if (demandDto.getDemandTitle() == null || demandDto.getDemandTitle().isBlank()) {
      errors.add("Demand title is required");
    }

    if (demandDto.getDepartment() == null || demandDto.getDepartment().isBlank()) {
      errors.add("Department is required");
    }

    if (demandDto.getLocation() == null || demandDto.getLocation().isBlank()) {
      errors.add("Location is required");
    }

    if (demandDto.getRequiredExperienceYears() == null) {
      errors.add("Required experience is required");
    } else if (demandDto.getRequiredExperienceYears() < 0) {
      errors.add("Required experience cannot be negative");
    }

    if (demandDto.getNumberOfOpenings() == null) {
      errors.add("Number of openings is required");
    } else if (demandDto.getNumberOfOpenings() < 1) {
      errors.add("Number of openings should be at least 1");
    }

    if (demandDto.getRequiredSkills() == null || demandDto.getRequiredSkills().isEmpty()) {
      errors.add("Required skills are required");
    }

    if (!errors.isEmpty()) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              errors
      );
    }
  }

  private void calculateSkillMatching(
          ApplicationDto dto,
          Demand demand
  ) {

    List<String> requiredSkills =
            demand.getRequiredSkills() == null
                    ? List.of()
                    : normalizeSkills(demand.getRequiredSkills());

    List<String> candidateSkills =
            dto.getCandidateSkills() == null
                    ? List.of()
                    : normalizeSkills(dto.getCandidateSkills());

    if (candidateSkills.isEmpty()) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Candidate skills are required"
      );
    }

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

    dto.setCandidateSkills(candidateSkills);
    dto.setMatchedSkills(matchedSkills);
    dto.setMissingSkills(missingSkills);
    dto.setOtherSkills(otherSkills);
    dto.setAiScore(calculateAiScore(requiredSkills, matchedSkills));
    dto.setAiRationale(buildAiRationale(matchedSkills, missingSkills, otherSkills));
  }

  private List<String> normalizeSkills(List<String> skills) {

    return skills.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(skill -> !skill.isBlank())
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
      return Stage.valueOf(stage.toUpperCase());
    } catch (Exception ex) {
      throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "Invalid stage: " + stage
      );
    }
  }}