package com.talentgrid.workforce.rmgsearch.service.impl;

import com.talentgrid.workforce.benchreport.dto.BenchEmployeeDto;
import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;
import com.talentgrid.workforce.benchreport.service.BenchReportService;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import com.talentgrid.workforce.rmgsearch.dto.RmgSearchRequest;
import com.talentgrid.workforce.rmgsearch.dto.RmgSearchResponse;
import com.talentgrid.workforce.rmgsearch.service.RmgSearchService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RmgSearchServiceImpl implements RmgSearchService {

    private final BenchReportService benchReportService;

    public RmgSearchServiceImpl(BenchReportService benchReportService) {
        this.benchReportService = benchReportService;
    }

    @Override
    public RmgSearchResponse search(RmgSearchRequest request, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }

        BenchReportResponse report = benchReportService.getBenchReport();

        List<BenchEmployeeDto> allBenchEmployees = new ArrayList<>();
        allBenchEmployees.addAll(report.getUnder30Days());
        allBenchEmployees.addAll(report.getThirtyToSixtyDays());
        allBenchEmployees.addAll(report.getSixtyToNinetyDays());

        List<BenchEmployeeDto> filtered = allBenchEmployees.stream()
                .filter(e -> matchesEmployeeId(e, request))
                .filter(e -> matchesName(e, request))
                .filter(e -> matchesEmail(e, request))
                .filter(e -> matchesSkills(e, request))
                .filter(e -> matchesAvailabilityFrom(e, request))
                .filter(e -> matchesAvailabilityTo(e, request))
                .filter(e -> matchesLocation(e, request))
                .filter(e -> matchesLevel(e, request))
                .filter(e -> matchesContractType(e, request))
                .sorted(Comparator.comparing(
                        BenchEmployeeDto::getAvailabilityDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());

        int totalResults = filtered.size();
        int totalPages = totalResults == 0 ? 0 : (int) Math.ceil((double) totalResults / size);
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<BenchEmployeeDto> results = filtered.subList(fromIndex, toIndex);

        return new RmgSearchResponse(results, totalResults, page, size, totalPages, Instant.now());
    }

    private boolean matchesEmployeeId(BenchEmployeeDto employee, RmgSearchRequest request) {
        if (request.getEmployeeId() == null || request.getEmployeeId().isBlank()) {
            return true;
        }
        String filter = request.getEmployeeId().trim().toLowerCase();
        return getEmployeeDisplayId(employee).toLowerCase().contains(filter);
    }

    private boolean matchesName(BenchEmployeeDto employee, RmgSearchRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            return true;
        }
        return employee.getName() != null
                && employee.getName().toLowerCase().contains(request.getName().trim().toLowerCase());
    }

    private boolean matchesEmail(BenchEmployeeDto employee, RmgSearchRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return true;
        }
        return employee.getEmail() != null
                && employee.getEmail().toLowerCase().contains(request.getEmail().trim().toLowerCase());
    }

    private String getEmployeeDisplayId(BenchEmployeeDto employee) {
        if (employee.getEmployeeCode() != null) {
            return String.valueOf(employee.getEmployeeCode());
        }
        return employee.getEmployeeId() != null ? String.valueOf(employee.getEmployeeId()) : "";
    }

    private boolean matchesSkills(BenchEmployeeDto employee, RmgSearchRequest request) {
        List<String> skillFilters = normalizeSkillFilters(request.getSkills());
        if (skillFilters.isEmpty()) {
            return true;
        }
        if (employee.getSkills() == null || employee.getSkills().isEmpty()) {
            return false;
        }
        return skillFilters.stream().anyMatch(filter ->
                employee.getSkills().stream()
                        .anyMatch(s -> s != null && s.trim().toLowerCase().equals(filter)));
    }

    private List<String> normalizeSkillFilters(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        return skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean matchesAvailabilityFrom(BenchEmployeeDto employee, RmgSearchRequest request) {
        if (request.getAvailabilityDateFrom() == null) {
            return true;
        }
        return employee.getAvailabilityDate() != null
                && !employee.getAvailabilityDate().isBefore(request.getAvailabilityDateFrom());
    }

    private boolean matchesAvailabilityTo(BenchEmployeeDto employee, RmgSearchRequest request) {
        if (request.getAvailabilityDateTo() == null) {
            return true;
        }
        return employee.getAvailabilityDate() != null
                && !employee.getAvailabilityDate().isAfter(request.getAvailabilityDateTo());
    }

    private boolean matchesLocation(BenchEmployeeDto employee, RmgSearchRequest request) {
        if (request.getLocation() == null || request.getLocation().isBlank()) {
            return true;
        }
        return employee.getLocation() != null
                && employee.getLocation().toLowerCase().contains(request.getLocation().trim().toLowerCase());
    }

    private boolean matchesLevel(BenchEmployeeDto employee, RmgSearchRequest request) {
        Level levelFilter = request.getLevel() != null ? request.getLevel() : request.getSeniority();
        if (levelFilter == null) {
            return true;
        }
        return levelFilter.equals(employee.getLevel());
    }

    private boolean matchesContractType(BenchEmployeeDto employee, RmgSearchRequest request) {
        if (request.getContractType() == null) {
            return true;
        }
        return request.getContractType().equals(employee.getContractType());
    }
}
