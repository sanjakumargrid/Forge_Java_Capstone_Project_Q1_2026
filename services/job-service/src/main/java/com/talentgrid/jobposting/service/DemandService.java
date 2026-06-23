package com.talentgrid.jobposting.service;

import com.talentgrid.jobposting.dto.response.DemandResponse;
import com.talentgrid.jobposting.entity.Demand;
import com.talentgrid.jobposting.event.DemandEvent;
import com.talentgrid.jobposting.event.DemandPayload;
import com.talentgrid.jobposting.exception.ResourceNotFoundException;
import com.talentgrid.jobposting.repository.DemandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandService {

    private final DemandRepository demandRepository;
    private final JobPostingService jobPostingService;

    /**
     * Upserts a demand from an inbound Kafka event. Demands arrive as full
     * snapshots of current state (not diffs), so any event type other than
     * DEMAND_CLOSED fully overwrites the stored row for that demandId — this
     * is what lets a demand move through OPEN_EXTERNAL, FILLED_*, etc. and
     * stay in sync without needing a special case per transition.
     */
    @Transactional
    public void processKafkaEvent(DemandEvent event) {
        DemandPayload p = event.getPayload();
        if (p == null || p.getDemandId() == null) {
            log.warn("Received demand event with null payload or demandId, skipping");
            return;
        }

        if ("DEMAND_CLOSED".equals(event.getEventType())) {
            log.info("Demand {} closed — closing linked job postings", p.getDemandId());
            demandRepository.findByDemandId(p.getDemandId()).ifPresent(d -> {
                d.setStatus("CLOSED");
                d.setClosureReason(p.getClosureReason());
                demandRepository.save(d);
                jobPostingService.closePostingsForDemand(d.getDemandId());
            });
            return;
        }

        if (p.getTitle() == null) {
            log.warn("Demand event for demandId={} has no title, skipping persist", p.getDemandId());
            return;
        }

        Demand demand = demandRepository.findByDemandId(p.getDemandId())
                .orElseGet(() -> Demand.builder().demandId(p.getDemandId()).build());

        demand.setTitle(p.getTitle());
        demand.setDescription(p.getDescription());
        demand.setLevel(p.getLevel());
        demand.setLocation(p.getLocation());
        demand.setEmploymentType(p.getEmploymentType());
        demand.setAccountId(p.getAccountId());
        demand.setProjectId(p.getProjectId());
        demand.setBusinessUnit(p.getBusinessUnit());
        demand.setSkills(p.getSkills());
        demand.setBudget(p.getBudget());
        demand.setRequiredCount(p.getRequiredCount());
        demand.setRecruitedCount(p.getRecruitedCount());
        demand.setInternalFilledCount(p.getInternalFilledCount());
        demand.setExternalFilledCount(p.getExternalFilledCount());
        demand.setStatus(p.getStatus());
        demand.setPriority(p.getPriority());
        demand.setPreviousStatus(p.getPreviousStatus());
        demand.setTargetDate(p.getTargetDate());
        demand.setSearchStartAt(p.getSearchStartAt());
        demand.setApprovedAt(p.getApprovedAt());
        demand.setSourceCreatedAt(p.getCreatedAt());
        demand.setSourceUpdatedAt(p.getUpdatedAt());
        demand.setClosureReason(p.getClosureReason());
        demand.setCreatedBy(p.getCreatedBy());
        demand.setAssignedRecruiter(p.getAssignedRecruiter());
        demand.setAssignedRm(p.getAssignedRm());
        demand.setApprovedBy(p.getApprovedBy());
        demand.setIsDeleted(Boolean.TRUE.equals(p.getIsDeleted()));
        demand.setVersion(p.getVersion());
        demand.setEventId(event.getEventId());
        demand.setCorrelationId(event.getCorrelationId());

        demandRepository.save(demand);
        log.info("Upserted demand from Kafka: demandId={} title={} status={}",
                p.getDemandId(), p.getTitle(), p.getStatus());
    }

    /** Returns every demand stored in the service. */
    @Transactional(readOnly = true)
    public List<DemandResponse> getAll() {
        return demandRepository.findAll()
                .stream()
                .map(DemandResponse::from)
                .toList();
    }

    /**
     * Returns only demands not yet tied to an in-progress or live job posting —
     * i.e. those still available for a recruiter to pick up.
     */
    @Transactional(readOnly = true)
    public List<DemandResponse> getAvailable() {
        return demandRepository.findAvailableDemands()
                .stream()
                .map(DemandResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DemandResponse getById(Long id) {
        Demand demand = demandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found: " + id));
        return DemandResponse.from(demand);
    }

    @Transactional(readOnly = true)
    public DemandResponse getByDemandId(Long demandId) {
        Demand demand = demandRepository.findByDemandId(demandId)
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found: " + demandId));
        return DemandResponse.from(demand);
    }
}
