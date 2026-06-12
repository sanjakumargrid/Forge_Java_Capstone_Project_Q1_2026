package com.talentgrid.application.application.mapper;

import com.talentgrid.application.application.dto.ApplicationDto;
import com.talentgrid.application.application.entity.Application;

import java.util.ArrayList;

public class ApplicationMapper {

    private ApplicationMapper() {
    }

    public static Application dtoToApplicationEntity(
            ApplicationDto dto
    ) {

        if (dto == null) {
            return null;
        }

        Application application = new Application();

        application.setId(
                dto.getApplicationId()
        );

        application.setCandidateId(
                dto.getCandidateId()
        );

        application.setDemandId(
                dto.getDemandId()
        );

        application.setSource(
                dto.getSource()
        );

        application.setResumeFilePath(
                dto.getResumeFilePath()
        );

        application.setResumeOriginalFilename(
                dto.getResumeOriginalFilename()
        );

        application.setMatchedSkills(
                dto.getMatchedSkills() == null
                        ? new ArrayList<>()
                        : dto.getMatchedSkills()
        );

        application.setMissingSkills(
                dto.getMissingSkills() == null
                        ? new ArrayList<>()
                        : dto.getMissingSkills()
        );

        application.setOtherSkills(
                dto.getOtherSkills() == null
                        ? new ArrayList<>()
                        : dto.getOtherSkills()
        );

        application.setAiScore(
                dto.getAiScore()
        );

        application.setAiRationale(
                dto.getAiRationale()
        );

        application.setCurrentStage(
                dto.getCurrentStage()
        );

        application.setStageMoveReason(
                dto.getStageMoveReason()
        );

        application.setFreeNotes(
                dto.getFreeNotes()
        );

        application.setReferralCode(
                dto.getReferralCode()
        );

        application.setBlockedFromReapply(
                dto.getBlockedFromReapply()
        );

        application.setRejectionReason(
                dto.getRejectionReason()
        );

        return application;
    }

    public static ApplicationDto applicationEntityToDto(
            Application application
    ) {

        if (application == null) {
            return null;
        }

        ApplicationDto dto = new ApplicationDto();

        dto.setApplicationId(
                application.getId()
        );

        dto.setCandidateId(
                application.getCandidateId()
        );

        dto.setDemandId(
                application.getDemandId()
        );

        dto.setSource(
                application.getSource()
        );

        dto.setResumeFilePath(
                application.getResumeFilePath()
        );

        dto.setResumeOriginalFilename(
                application.getResumeOriginalFilename()
        );

        dto.setMatchedSkills(
                application.getMatchedSkills()
        );

        dto.setMissingSkills(
                application.getMissingSkills()
        );

        dto.setOtherSkills(
                application.getOtherSkills()
        );

        dto.setAiScore(
                application.getAiScore()
        );

        dto.setAiRationale(
                application.getAiRationale()
        );

        dto.setCurrentStage(
                application.getCurrentStage()
        );

        dto.setStageMoveReason(
                application.getStageMoveReason()
        );

        dto.setFreeNotes(
                application.getFreeNotes()
        );

        dto.setReferralCode(
                application.getReferralCode()
        );

        dto.setBlockedFromReapply(
                application.getBlockedFromReapply()
        );

        dto.setAppliedAt(
                application.getAppliedAt()
        );

        dto.setScreeningAt(
                application.getScreeningAt()
        );

        dto.setTechnicalAt(
                application.getTechnicalAt()
        );

        dto.setInterviewAt(
                application.getInterviewAt()
        );

        dto.setFinalRoundAt(
                application.getFinalRoundAt()
        );

        dto.setOfferAt(
                application.getOfferAt()
        );

        dto.setHiredAt(
                application.getHiredAt()
        );

        dto.setRejectedAt(
                application.getRejectedAt()
        );

        dto.setRejectionReason(
                application.getRejectionReason()
        );

        return dto;
    }
}