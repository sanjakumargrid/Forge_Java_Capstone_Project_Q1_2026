package com.talentgrid.workforce.rmgdashboard.service;

import feign.FeignException;
import com.talentgrid.workforce.rmgdashboard.client.DemandClient;
import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.dto.DemandStatusTransitionRequest;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSummaryDto;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSummaryPageResponse;
import com.talentgrid.workforce.rmgdashboard.mapper.DemandStatusMapper;
import com.talentgrid.workforce.rmgdashboard.mapper.DemandStatusMapper.MappedTransition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RmgService {

    private final DemandClient demandClient;

    public Page<DemandDto> getDemandsByStatus(String status, Pageable pageable) {
        // Backward compatibility method - convert single status to list
        List<String> statuses = (status != null && !status.trim().isEmpty()) 
            ? List.of(status.trim()) 
            : null;
        return getDemandsByStatuses(statuses, pageable);
    }

    public Page<DemandDto> getDemandsByStatuses(List<String> statuses, Pageable pageable) {
        log.info("Fetching demands from demand-service with statuses={}, page={}, size={}",
                statuses, pageable.getPageNumber(), pageable.getPageSize());

        DemandSummaryPageResponse summaryPage = demandClient.getDemandsByStatuses(
                statuses,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        List<DemandSummaryDto> summaries = summaryPage != null && summaryPage.getContent() != null
                ? summaryPage.getContent()
                : List.of();

        List<DemandDto> demandsByStatus = summaries.stream()
                .map(this::toDemandDtoFromSummary)
                .toList();

        long totalElements = summaryPage != null ? summaryPage.getTotalElements() : demandsByStatus.size();
        return new PageImpl<>(demandsByStatus, pageable, totalElements);
    }

    public DemandDto updateDemandStatus(Long demandId, String status) {
        return updateDemandStatus(demandId, status, null, null);
    }

    public DemandDto updateDemandStatus(Long demandId, String status, String closureReason, String comments) {
        log.info("Updating demand {} status to {} (closureReason={}, comments={})",
                demandId, status, closureReason, comments);

        MappedTransition mapped = DemandStatusMapper.toDemandService(status, closureReason);
        log.debug("Mapped demand transition for demand {}: targetStatus={}, closureReason={}",
                demandId, mapped.targetStatus(), mapped.closureReason());

        DemandStatusTransitionRequest feignRequest = DemandStatusTransitionRequest.builder()
                .targetStatus(mapped.targetStatus())
                .closureReason(mapped.closureReason())
                .comments(comments)
                .build();
        try {
            return demandClient.updateDemandStatus(demandId, feignRequest);
        } catch (FeignException ex) {
            HttpStatus statusCode = HttpStatus.resolve(ex.status());
            HttpStatus resolved = statusCode != null ? statusCode : HttpStatus.BAD_GATEWAY;
            String message = ex.contentUTF8() != null && !ex.contentUTF8().isBlank()
                    ? ex.contentUTF8()
                    : ex.getMessage();
            throw new ResponseStatusException(resolved, message, ex);
        }
    }

    private DemandDto toDemandDtoFromSummary(DemandSummaryDto summary) {
        if (summary == null) {
            return null;
        }
        return DemandDto.builder()
                .demandId(summary.getDemandId())
                .title(summary.getTitle())
                .description(summary.getDescription())
                .level(summary.getLevel())
                .location(summary.getLocation())
                .projectId(summary.getProjectId())
                .projectName(summary.getProjectName())
                .accountId(summary.getAccountId())
                .accountName(summary.getAccountName())
                .businessUnit(summary.getBusinessUnit())
                // Map the new skill structure
                .mandatorySkills(summary.getMandatorySkills())
                .optionalSkills(summary.getOptionalSkills())
                // Legacy skills field for backward compatibility (use combined skills)
                .skills(summary.getAllSkillNames())
                .budget(summary.getBudget())
                .employmentType(summary.getEmploymentType())
                .status(summary.getStatus())
                .priority(summary.getPriority())
                .previousStatus(summary.getPreviousStatus())
                .targetDate(summary.getTargetDate())
                .searchStartAt(summary.getSearchStartAt())
                .approvedAt(summary.getApprovedAt())
                .closureReason(summary.getClosureReason())
                .createdBy(summary.getCreatedBy())
                .creatorName(summary.getCreatorName())
                .creatorEmail(summary.getCreatorEmail())
                .assignedRecruiter(summary.getAssignedRecruiter())
                .assignedRecruiterName(summary.getAssignedRecruiterName())
                .assignedRm(summary.getAssignedRm())
                .assignedRmName(summary.getAssignedRmName())
                .approvedBy(summary.getApprovedBy())
                .approverName(summary.getApproverName())
                .requiredCount(summary.getRequiredCount())
                .recruitedCount(summary.getRecruitedCount())
                .internalFilledCount(summary.getInternalFilledCount())
                .externalFilledCount(summary.getExternalFilledCount())
                .isDeleted(summary.getIsDeleted())
                .version(summary.getVersion())
                .createdAt(summary.getCreatedAt())
                .updatedAt(summary.getUpdatedAt())
                .build();
    }
}
