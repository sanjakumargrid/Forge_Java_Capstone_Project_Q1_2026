package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.enums.EmploymentType;
import com.talentgrid.demand.dto.response.DemandPipelineResponse;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.dto.response.DemandStatusHistoryResponse;
import com.talentgrid.demand.dto.response.DemandSummaryResponse;
import com.talentgrid.demand.exception.DemandNotFoundException;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import com.talentgrid.demand.util.SecurityUtils;

/**
 * Handles demand read operations: detail views, enterprise search
 * with filters/sorts, and unified pipeline aggregation.
 *
 * <p>Performance notes:
 * <ul>
 *   <li>Dashboard queries use summary projections to avoid loading TEXT/ARRAY columns.</li>
 *   <li>Composite DB indexes on (status, priority, business_unit, is_deleted) ensure &lt; 2s SLA.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class DemandQueryService {

    private final DemandRepository demandRepository;
    private final DemandMapper demandMapper;

    /**
     * Retrieves a single demand by ID with full detail.
     *
     * @param id the demand ID
     * @return full demand response DTO
     * @throws DemandNotFoundException if the demand does not exist or is soft-deleted
     */
    public DemandResponse getDemandById(Long id) {
        Demand demand = demandRepository.findByDemandIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new DemandNotFoundException(
                        "Demand not found with id: " + id));
        log.info("Fetched demand details for id={}", id);
        return demandMapper.toResponse(demand);
    }

    /**
     * Enterprise demand search with optional filters and sorting.
     *
     * <p>Supports filtering by:
     * <ul>
     *   <li>status — demand lifecycle state</li>
     *   <li>priority — demand priority level</li>
     *   <li>businessUnit — organizational unit</li>
     * </ul>
     *
     * <p>Supports sorting by:
     * <ul>
     *   <li>{@code age} — sort by created_at (ascending = oldest first)</li>
     *   <li>{@code priority} — sort by priority enum ordinal</li>
     *   <li>Default: created_at descending (newest first)</li>
     * </ul>
     *
     * @param statuses     optional status filter — one or more values, e.g. APPROVED, INTERNAL_SEARCH
     * @param priority     optional priority filter
     * @param businessUnit optional business unit filter
     * @param sortBy       sort field: "age" or "priority" (default: created_at desc)
     * @param sortDir      sort direction: "asc" or "desc" (default: "desc")
     * @param page         zero-based page index
     * @param size         page size (max 100)
     * @return page of demand summary responses
     */
    public Page<DemandSummaryResponse> searchDemands(List<DemandStatus> statuses,
                                                      DemandPriority priority,
                                                      String businessUnit,
                                                      String accountName,
                                                      String location,
                                                      EmploymentType employmentType,
                                                      String sortBy,
                                                      String sortDir,
                                                      int page,
                                                      int size) {
        // Cap page size to 100 for performance
        int effectiveSize = Math.min(size, 100);
        Sort sort = resolveSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, effectiveSize, sort);

        Specification<Demand> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Base filter
            predicates.add(cb.isFalse(root.get("isDeleted")));

            // Optional status filter — supports one or more statuses (IN predicate)
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (businessUnit != null) {
                predicates.add(cb.equal(root.get("businessUnit"), businessUnit));
            }
            if (accountName != null) {
                predicates.add(cb.equal(root.get("accountName"), accountName));
            }
            if (location != null) {
                predicates.add(cb.equal(root.get("location"), location));
            }
            if (employmentType != null) {
                predicates.add(cb.equal(root.get("employmentType"), employmentType));
            }

            // Role-based visibility logic
            if (!SecurityUtils.hasAnyRole("ADMIN", "RMG")) {
                if (SecurityUtils.hasAnyRole("RECRUITER")) {
                    predicates.add(root.get("status").in(
                        DemandStatus.OPEN_EXTERNAL,
                        DemandStatus.FILLED,
                        DemandStatus.CLOSED
                    ));
                } else if (SecurityUtils.isHiringManager()) {
                    Long userAccountId = SecurityUtils.getCurrentUserAccountId();
                    if (userAccountId != null) {
                        predicates.add(cb.or(
                            cb.equal(root.get("accountId"), userAccountId),
                            cb.equal(root.get("status"), DemandStatus.OPEN_EXTERNAL)
                        ));
                    } else {
                        // Fallback to only their own created demands + OPEN_EXTERNAL
                        predicates.add(cb.or(
                            cb.equal(root.get("createdBy"), SecurityUtils.getCurrentUserId()),
                            cb.equal(root.get("status"), DemandStatus.OPEN_EXTERNAL)
                        ));
                    }
                } else if (SecurityUtils.hasAnyRole("EMPLOYEE")) {
                    predicates.add(cb.equal(root.get("status"), DemandStatus.OPEN_EXTERNAL));
                } else {
                    // Default fallback
                    predicates.add(cb.or(
                        cb.equal(root.get("createdBy"), SecurityUtils.getCurrentUserId()),
                        cb.equal(root.get("status"), DemandStatus.OPEN_EXTERNAL)
                    ));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Demand> demandPage = demandRepository.findAll(spec, pageable);
        return demandPage.map(demandMapper::toSummaryResponse);
    }

    /**
     * Returns the unified internal and external hiring pipeline for a demand.
     * Includes status history (audit trail) and fill count breakdowns.
     *
     * @param id the demand ID
     * @return pipeline response with both internal and external tracking
     * @throws DemandNotFoundException if the demand does not exist or is soft-deleted
     */
    public DemandPipelineResponse getPipeline(Long id) {
        Demand demand = demandRepository.findByDemandIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new DemandNotFoundException(
                        "Demand not found with id: " + id));

        DemandPipelineResponse pipeline = new DemandPipelineResponse();
        pipeline.setDemandId(demand.getDemandId());
        pipeline.setTitle(demand.getTitle());
        pipeline.setStatus(demand.getStatus() != null ? demand.getStatus().name() : null);
        pipeline.setIsFilled(demand.getIsFilled());
        pipeline.setFillType(demand.getFillType() != null ? demand.getFillType().name() : null);

        // Map status history as the audit trail
        List<DemandStatusHistory> histories = demand.getStatusHistories();
        if (histories != null) {
            List<DemandStatusHistoryResponse> historyResponses = demandMapper.toHistoryResponseList(histories);
            pipeline.setStatusHistory(historyResponses);
        }

        return pipeline;
    }

    /**
     * Returns the full status history audit trail for a demand.
     *
     * @param id the demand ID
     * @return list of status history responses
     * @throws DemandNotFoundException if the demand does not exist
     */
    public List<DemandStatusHistoryResponse> getDemandHistory(Long id) {
        Demand demand = demandRepository.findByDemandIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new DemandNotFoundException(
                        "Demand not found with id: " + id));

        List<DemandStatusHistory> histories = demand.getStatusHistories();
        if (histories == null) {
            return List.of();
        }
        return demandMapper.toHistoryResponseList(histories);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    /**
     * Resolves the sort criteria based on the requested sort field and direction.
     */
    private Sort resolveSort(String sortBy, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        if ("age".equalsIgnoreCase(sortBy)) {
            // Sort by age = sort by createdAt (ASC = oldest first)
            return Sort.by(direction, "createdAt");
        } else if ("priority".equalsIgnoreCase(sortBy)) {
            return Sort.by(direction, "priority");
        }

        // Default sort: newest first
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
