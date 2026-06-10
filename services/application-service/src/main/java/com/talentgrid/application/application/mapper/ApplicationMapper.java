package com.talentgrid.application.application.mapper;

import com.forge.talentAcquisitionEngine.applicationService.application.dto.ApplicationDto;
import com.forge.talentAcquisitionEngine.applicationService.application.entity.Application;
import com.forge.talentAcquisitionEngine.candidateService.externalCandidate.entity.ExternalCandidate;
import com.forge.talentAcquisitionEngine.interviewService.interview.entity.Interview;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationMapper {

  private ApplicationMapper() {
  }

  public static Application dtoToEntity(
      ApplicationDto dto,
      ExternalCandidate candidate
  ) {

    if (dto == null) {
      return null;
    }

    Application entity = new Application();

    entity.setId(dto.getId());

    entity.setCandidate(candidate);

    entity.setSource(dto.getSource());

    entity.setResumeFilePath(
        dto.getResumeFilePath()
    );

    entity.setResumeOriginalFilename(
        dto.getResumeOriginalFilename()
    );

    entity.setMatchedSkills(
        dto.getMatchedSkills()
    );

    entity.setMissingSkills(
        dto.getMissingSkills()
    );

    entity.setOtherSkills(
        dto.getOtherSkills()
    );

    entity.setAiScore(
        dto.getAiScore()
    );

    entity.setAiRationale(
        dto.getAiRationale()
    );

    entity.setCurrentStage(
        dto.getCurrentStage()
    );

    entity.setStageMoveReason(
        dto.getStageMoveReason()
    );

    entity.setFreeNotes(
        dto.getFreeNotes()
    );

    entity.setReferralCode(
        dto.getReferralCode()
    );

    entity.setBlockedFromReapply(
        dto.getBlockedFromReapply()
    );

    entity.setAppliedAt(
        dto.getAppliedAt()
    );

    entity.setScreeningAt(
        dto.getScreeningAt()
    );

    entity.setTechnicalAt(
        dto.getTechnicalAt()
    );

    entity.setInterviewAt(
        dto.getInterviewAt()
    );

    entity.setReapplyAllowedAfter(
        dto.getReapplyAllowedAfter()
    );

    entity.setFinalRoundAt(
        dto.getFinalRoundAt()
    );

    entity.setOfferAt(
        dto.getOfferAt()
    );

    entity.setHiredAt(
        dto.getHiredAt()
    );

    entity.setRejectedAt(
        dto.getRejectedAt()
    );

    entity.setRejectionReason(
        dto.getRejectionReason()
    );

    return entity;
  }

  public static ApplicationDto entityToDto(
      Application entity
  ) {

    if (entity == null) {
      return null;
    }

    ApplicationDto dto = new ApplicationDto();

    dto.setId(
        entity.getId()
    );

    if (entity.getCandidate() != null) {

      dto.setCandidateId(
          entity.getCandidate().getCandidateId()
      );
    }

    dto.setSource(
        entity.getSource()
    );

    dto.setResumeFilePath(
        entity.getResumeFilePath()
    );

    dto.setResumeOriginalFilename(
        entity.getResumeOriginalFilename()
    );

    dto.setMatchedSkills(
        entity.getMatchedSkills()
    );

    dto.setMissingSkills(
        entity.getMissingSkills()
    );

    dto.setOtherSkills(
        entity.getOtherSkills()
    );

    dto.setAiScore(
        entity.getAiScore()
    );

    dto.setAiRationale(
        entity.getAiRationale()
    );

    dto.setCurrentStage(
        entity.getCurrentStage()
    );

    dto.setStageMoveReason(
        entity.getStageMoveReason()
    );

    dto.setFreeNotes(
        entity.getFreeNotes()
    );

    dto.setReferralCode(
        entity.getReferralCode()
    );

    dto.setBlockedFromReapply(
        entity.getBlockedFromReapply()
    );

    dto.setAppliedAt(
        entity.getAppliedAt()
    );

    dto.setScreeningAt(
        entity.getScreeningAt()
    );

    dto.setTechnicalAt(
        entity.getTechnicalAt()
    );

    dto.setInterviewAt(
        entity.getInterviewAt()
    );

    dto.setFinalRoundAt(
        entity.getFinalRoundAt()
    );

    dto.setOfferAt(
        entity.getOfferAt()
    );

    dto.setHiredAt(
        entity.getHiredAt()
    );

    dto.setRejectedAt(
        entity.getRejectedAt()
    );

    dto.setReapplyAllowedAfter(
        entity.getReapplyAllowedAfter()
    );
    dto.setRejectionReason(
        entity.getRejectionReason()
    );

    if (entity.getInterviews() != null) {

      List<Long> interviewIds =
          entity.getInterviews()
              .stream()
              .map(Interview::getId)
              .collect(Collectors.toList());

      dto.setInterviewIds(
          interviewIds
      );

    } else {

      dto.setInterviewIds(
          new ArrayList<>()
      );
    }

    return dto;
  }
}