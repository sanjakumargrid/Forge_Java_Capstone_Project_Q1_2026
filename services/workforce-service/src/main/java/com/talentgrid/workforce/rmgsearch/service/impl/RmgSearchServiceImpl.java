package com.talentgrid.workforce.rmgsearch.service.impl;

import com.talentgrid.workforce.benchreport.dto.BenchEmployeeDto;
import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;
import com.talentgrid.workforce.benchreport.service.BenchReportService;
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
    public RmgSearchResponse search(RmgSearchRequest request) {
        BenchReportResponse report = benchReportService.getBenchReport();

        List<BenchEmployeeDto> allBenchEmployees = new ArrayList<>();
        allBenchEmployees.addAll(report.getUnder30Days());
        allBenchEmployees.addAll(report.getThirtyToSixtyDays());
        allBenchEmployees.addAll(report.getSixtyToNinetyDays());

        List<BenchEmployeeDto> results = allBenchEmployees.stream()
                .filter(e -> matchesSkill(e, request))
                .filter(e -> matchesAvailabilityFrom(e, request))
                .filter(e -> matchesAvailabilityTo(e, request))
                .filter(e -> matchesLocation(e, request))
                .filter(e -> matchesSeniority(e, request))
                .filter(e -> matchesContractType(e, request))
                .sorted(Comparator.comparing(
                        BenchEmployeeDto::getAvailabilityDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());

        return new RmgSearchResponse(results, results.size(), Instant.now());
    }

    private boolean matchesSkill(BenchEmployeeDto employee, RmgSearchRequest request) {
        if (request.getSkill() == null || request.getSkill().isBlank()) {
            return true;
        }
        if (employee.getSkills() == null || employee.getSkills().isEmpty()) {
            return false;
        }
        String skillFilter = request.getSkill().trim().toLowerCase();
        return employee.getSkills().stream()
                .anyMatch(s -> s != null && s.trim().toLowerCase().equals(skillFilter));
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

    private boolean matchesSeniority(BenchEmployeeDto employee, RmgSearchRequest request) {
        if (request.getSeniority() == null) {
            return true;
        }
        return request.getSeniority().equals(employee.getLevel());
    }

    private boolean matchesContractType(BenchEmployeeDto employee, RmgSearchRequest request) {
        if (request.getContractType() == null) {
            return true;
        }
        return request.getContractType().equals(employee.getContractType());
    }
}
