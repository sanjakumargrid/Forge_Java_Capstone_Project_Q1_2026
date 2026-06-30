package com.talentgrid.workforce.rmgdashboard.service.impl;

import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSearchRequest;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSearchResponse;
import com.talentgrid.workforce.rmgdashboard.service.DemandSearchService;
import com.talentgrid.workforce.rmgdashboard.service.RmgService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class DemandSearchServiceImpl implements DemandSearchService {

    private static final int FETCH_SIZE = 500;

    private static final Map<String, Integer> PRIORITY_ORDER = Map.of(
            "CRITICAL", 0,
            "HIGH", 1,
            "MEDIUM", 2,
            "LOW", 3
    );

    private final RmgService rmgService;

    public DemandSearchServiceImpl(RmgService rmgService) {
        this.rmgService = rmgService;
    }

    @Override
    public DemandSearchResponse search(DemandSearchRequest request) {
        List<String> statuses = resolveStatuses(request);
        Page<DemandDto> page = rmgService.getDemandsByStatuses(statuses, PageRequest.of(0, FETCH_SIZE));

        List<DemandDto> results = page.getContent().stream()
                .filter(d -> matchesDemandId(d, request))
                .filter(d -> matchesTitle(d, request))
                .filter(d -> matchesSkill(d, request))
                .filter(d -> matchesLocation(d, request))
                .filter(d -> matchesLevel(d, request))
                .filter(d -> matchesPriority(d, request))
                .filter(d -> matchesAccountName(d, request))
                .filter(d -> matchesBusinessUnit(d, request))
                .filter(d -> matchesEmploymentType(d, request))
                .filter(d -> matchesAssignedRm(d, request))
                .filter(d -> matchesTargetDateFrom(d, request))
                .filter(d -> matchesTargetDateTo(d, request))
                .sorted(Comparator
                        .comparing(DemandDto::getTargetDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(this::priorityRank))
                .toList();

        return new DemandSearchResponse(results, results.size(), Instant.now());
    }

    private List<String> resolveStatuses(DemandSearchRequest request) {
        List<String> resolved = new ArrayList<>();
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            resolved.add(request.getStatus().trim());
        }
        if (request.getStatuses() != null) {
            request.getStatuses().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .forEach(resolved::add);
        }
        return resolved.isEmpty() ? null : resolved;
    }

    private boolean matchesDemandId(DemandDto demand, DemandSearchRequest request) {
        if (request.getDemandId() == null) {
            return true;
        }
        return request.getDemandId().equals(demand.getDemandId());
    }

    private boolean matchesTitle(DemandDto demand, DemandSearchRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            return true;
        }
        return demand.getTitle() != null
                && demand.getTitle().toLowerCase().contains(request.getTitle().trim().toLowerCase());
    }

    private boolean matchesSkill(DemandDto demand, DemandSearchRequest request) {
        if (request.getSkill() == null || request.getSkill().isBlank()) {
            return true;
        }
        String skillFilter = request.getSkill().trim().toLowerCase();
        return demand.getAllSkillNames().stream()
                .anyMatch(s -> s != null && s.trim().toLowerCase().equals(skillFilter));
    }

    private boolean matchesLocation(DemandDto demand, DemandSearchRequest request) {
        if (request.getLocation() == null || request.getLocation().isBlank()) {
            return true;
        }
        return demand.getLocation() != null
                && demand.getLocation().toLowerCase().contains(request.getLocation().trim().toLowerCase());
    }

    private boolean matchesLevel(DemandDto demand, DemandSearchRequest request) {
        if (request.getLevel() == null) {
            return true;
        }
        return demand.getLevel() != null
                && request.getLevel().name().equalsIgnoreCase(demand.getLevel().trim());
    }

    private boolean matchesPriority(DemandDto demand, DemandSearchRequest request) {
        if (request.getPriority() == null || request.getPriority().isBlank()) {
            return true;
        }
        return demand.getPriority() != null
                && demand.getPriority().equalsIgnoreCase(request.getPriority().trim());
    }

    private boolean matchesAccountName(DemandDto demand, DemandSearchRequest request) {
        if (request.getAccountName() == null || request.getAccountName().isBlank()) {
            return true;
        }
        return demand.getAccountName() != null
                && demand.getAccountName().toLowerCase().contains(request.getAccountName().trim().toLowerCase());
    }

    private boolean matchesBusinessUnit(DemandDto demand, DemandSearchRequest request) {
        if (request.getBusinessUnit() == null || request.getBusinessUnit().isBlank()) {
            return true;
        }
        return demand.getBusinessUnit() != null
                && demand.getBusinessUnit().equalsIgnoreCase(request.getBusinessUnit().trim());
    }

    private boolean matchesEmploymentType(DemandDto demand, DemandSearchRequest request) {
        if (request.getEmploymentType() == null) {
            return true;
        }
        return demand.getEmploymentType() != null
                && request.getEmploymentType().name().equalsIgnoreCase(demand.getEmploymentType().trim());
    }

    private boolean matchesAssignedRm(DemandDto demand, DemandSearchRequest request) {
        if (request.getAssignedRm() == null) {
            return true;
        }
        return request.getAssignedRm().equals(demand.getAssignedRm());
    }

    private boolean matchesTargetDateFrom(DemandDto demand, DemandSearchRequest request) {
        if (request.getTargetDateFrom() == null) {
            return true;
        }
        return demand.getTargetDate() != null
                && !demand.getTargetDate().isBefore(request.getTargetDateFrom());
    }

    private boolean matchesTargetDateTo(DemandDto demand, DemandSearchRequest request) {
        if (request.getTargetDateTo() == null) {
            return true;
        }
        return demand.getTargetDate() != null
                && !demand.getTargetDate().isAfter(request.getTargetDateTo());
    }

    private int priorityRank(DemandDto demand) {
        if (demand.getPriority() == null) {
            return Integer.MAX_VALUE;
        }
        return PRIORITY_ORDER.getOrDefault(demand.getPriority().toUpperCase(), Integer.MAX_VALUE - 1);
    }
}
