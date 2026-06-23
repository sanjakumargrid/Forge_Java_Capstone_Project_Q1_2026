package com.talentgrid.demand.service;

import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.client.dto.ManagedProjectDto;
import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.dto.response.DemandSummaryResponse;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.util.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-only queries for demands on projects where the current user is PM
 * ({@code projects.project_manager_id} in user-auth).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PmDemandQueryService {

    private final DemandRepository demandRepository;
    private final DemandMapper demandMapper;
    private final UserAuthServiceClient userAuthServiceClient;

    public Page<DemandSummaryResponse> searchForCurrentPm(
            Long projectIdFilter,
            String sortBy,
            String sortDir,
            int page,
            int size) {

        Long pmUserId = SecurityUtils.getCurrentUserId();
        List<ManagedProjectDto> managed = userAuthServiceClient.getMyProjectsAsPm();
        if (managed == null) {
            managed = List.of();
        }

        Set<Long> allowedProjectIds = managed.stream()
                .map(ManagedProjectDto::getId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        if (allowedProjectIds.isEmpty()) {
            log.info("PM demand search: userId={} has no managed projects — returning empty page", pmUserId);
            return Page.empty(PageRequest.of(page, Math.min(size, 100)));
        }

        if (projectIdFilter != null && !allowedProjectIds.contains(projectIdFilter)) {
            log.warn("PM demand search: userId={} requested forbidden projectId={}", pmUserId, projectIdFilter);
            return Page.empty(PageRequest.of(page, Math.min(size, 100)));
        }

        int effectiveSize = Math.min(size, 100);
        Sort sort = resolveSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, effectiveSize, sort);

        Specification<Demand> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            predicates.add(root.get("projectId").in(allowedProjectIds));

            if (projectIdFilter != null) {
                predicates.add(cb.equal(root.get("projectId"), projectIdFilter));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return demandRepository.findAll(spec, pageable).map(demandMapper::toSummaryResponse);
    }

    private Sort resolveSort(String sortBy, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        if ("age".equalsIgnoreCase(sortBy)) {
            return Sort.by(direction, "createdAt");
        }
        if ("priority".equalsIgnoreCase(sortBy)) {
            return Sort.by(direction, "priority");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
