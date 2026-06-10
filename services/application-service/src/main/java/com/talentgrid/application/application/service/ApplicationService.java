package com.talentgrid.application.application.service;

import com.forge.talentAcquisitionEngine.applicationService.application.dto.ApplicationDto;
import com.forge.talentAcquisitionEngine.applicationService.application.entity.Application;
import com.forge.talentAcquisitionEngine.applicationService.application.enums.Stage;
import com.forge.talentAcquisitionEngine.applicationService.application.mapper.ApplicationMapper;
import com.forge.talentAcquisitionEngine.applicationService.application.repository.ApplicationRepository;
import com.forge.talentAcquisitionEngine.candidateService.externalCandidate.entity.ExternalCandidate;
import com.forge.talentAcquisitionEngine.candidateService.externalCandidate.repository.ExternalCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplicationService {

  @Autowired
  private final ApplicationRepository applicationRepository;

  private final ExternalCandidateRepository externalCandidateRepository;

  // =====================================================
  // GET APPLICATIONS
  // =====================================================

  public Page<ApplicationDto> getApplications(
          Long demandId,
          String stage,
          int page,
          int size
  ) {

    Pageable pageable =
            PageRequest.of(page, size);

    Page<Application> applications =
            applicationRepository.findAll(pageable);

    return applications.map(
            ApplicationMapper::entityToDto
    );
  }

  // =====================================================
  // CREATE APPLICATION
  // =====================================================

  public ApplicationDto createApplication(
          ApplicationDto dto
  ) {

    ExternalCandidate candidate =
            externalCandidateRepository.findById(
                    dto.getCandidateId()
            ).orElseThrow(
                    () -> new RuntimeException("Candidate not found")
            );

    Application application =
            ApplicationMapper.dtoToEntity(
                    dto,
                    candidate
            );

    Application saved =
            applicationRepository.save(
                    application
            );

    return ApplicationMapper.entityToDto(
            saved
    );
  }

  // =====================================================
  // BULK ACTION
  // =====================================================

  public String bulkAction(
          Map<String, Object> request
  ) {

    return "Bulk action completed";
  }

  // =====================================================
  // GET APPLICATION DETAILS
  // =====================================================

  public ApplicationDto getApplicationDetails(
          Long applicationId
  ) {

    Application application =
            applicationRepository.findById(
                    applicationId
            ).orElseThrow(
                    () -> new RuntimeException("Application not found")
            );

    return ApplicationMapper.entityToDto(
            application
    );
  }

  // =====================================================
  // MOVE STAGE
  // =====================================================

  public ApplicationDto moveStage(
          Long applicationId,
          Map<String, String> request
  ) {

    Application application =
            applicationRepository.findById(
                    applicationId
            ).orElseThrow(
                    () -> new RuntimeException("Application not found")
            );

    String targetStage =
            request.get("targetStage");

    application.setCurrentStage(
            Stage.valueOf(targetStage)
    );

    Application updated =
            applicationRepository.save(
                    application
            );

    return ApplicationMapper.entityToDto(
            updated
    );
  }

  // =====================================================
  // WITHDRAW APPLICATION
  // =====================================================

  public ApplicationDto withdrawApplication(
          Long applicationId,
          Map<String, String> request
  ) {

    Application application =
            applicationRepository.findById(
                    applicationId
            ).orElseThrow(
                    () -> new RuntimeException("Application not found")
            );

    application.setCurrentStage(
            Stage.REJECTED
    );

    application.setRejectionReason(
            request.get("reason")
    );

    Application updated =
            applicationRepository.save(
                    application
            );

    return ApplicationMapper.entityToDto(
            updated
    );
  }

  // =====================================================
  // TIMELINE
  // =====================================================

  public List<String> getTimeline(
          Long applicationId
  ) {

    return List.of(
            "APPLIED",
            "SCREENING",
            "INTERVIEW"
    );
  }

  // =====================================================
  // SEARCH APPLICATIONS
  // =====================================================

  public Page<ApplicationDto> searchApplications(
          String keyword,
          Integer minScore,
          int page,
          int size
  ) {

    Pageable pageable =
            PageRequest.of(page, size);

    Page<Application> applications =
            applicationRepository.findByAiScoreGreaterThanEqual(
                    minScore == null ? 0 : minScore,
                    pageable
            );

    return applications.map(
            ApplicationMapper::entityToDto
    );
  }

  // =====================================================
  // EXPORT APPLICATIONS
  // =====================================================

  public String exportApplications(
          Map<String, Object> request
  ) {

    return "Export started";
  }

}