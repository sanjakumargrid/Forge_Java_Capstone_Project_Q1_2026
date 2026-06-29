package com.talentgrid.workforce.benchreport.service.impl;

import com.talentgrid.workforce.benchreport.dto.BenchEmployeeDto;
import com.talentgrid.workforce.benchreport.dto.BenchReportPageResponse;
import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;
import com.talentgrid.workforce.benchreport.dto.BenchWindowPage;
import com.talentgrid.workforce.benchreport.repository.BenchReportRepository;
import com.talentgrid.workforce.benchreport.service.BenchReportService;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BenchReportServiceImpl implements BenchReportService {

    private final BenchReportRepository benchReportRepository;

    public BenchReportServiceImpl(BenchReportRepository benchReportRepository) {
        this.benchReportRepository = benchReportRepository;
    }

    @Override
    public BenchReportResponse getBenchReport() {
        return buildBenchReport(LocalDate.now());
    }

    @Override
    public BenchReportPageResponse getBenchReport(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }

        BenchReportResponse report = buildBenchReport(LocalDate.now());
        BenchReportPageResponse response = new BenchReportPageResponse();
        response.setRefreshedAt(report.getRefreshedAt());
        response.setUnder30Days(paginateWindow(report.getUnder30Days(), page, size));
        response.setThirtyToSixtyDays(paginateWindow(report.getThirtyToSixtyDays(), page, size));
        response.setSixtyToNinetyDays(paginateWindow(report.getSixtyToNinetyDays(), page, size));
        return response;
    }

    /**
     * Kept for backward compatibility. Report is built live from the database on each request.
     */
    @Override
    public void refreshBenchReport() {
        // no-op: caching removed so every API call reflects current internal_employees data
    }

    BenchReportResponse buildBenchReport(LocalDate today) {
        LocalDate ninetyDaysOut = today.plusDays(90);

        List<InternalEmployee> employees = benchReportRepository
                .findByAvailabilityDateBetweenAndIsDeletedFalseOrderByAvailabilityDateAsc(today, ninetyDaysOut);

        BenchReportResponse response = new BenchReportResponse();
        response.setRefreshedAt(Instant.now());
        response.setUnder30Days(groupEmployeesWithinRange(employees, today, today.plusDays(30)));
        response.setThirtyToSixtyDays(groupEmployeesWithinRange(employees, today.plusDays(31), today.plusDays(60)));
        response.setSixtyToNinetyDays(groupEmployeesWithinRange(employees, today.plusDays(61), ninetyDaysOut));
        return response;
    }

    private BenchWindowPage paginateWindow(List<BenchEmployeeDto> employees, int page, int size) {
        List<BenchEmployeeDto> source = employees != null ? employees : List.of();
        long totalElements = source.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, source.size());
        int toIndex = Math.min(fromIndex + size, source.size());

        return new BenchWindowPage(
                source.subList(fromIndex, toIndex),
                page,
                size,
                totalElements,
                totalPages
        );
    }

    private List<BenchEmployeeDto> groupEmployeesWithinRange(
            List<InternalEmployee> employees,
            LocalDate start,
            LocalDate end
    ) {
        return employees.stream()
                .filter(employee -> employee.getAvailabilityDate() != null)
                .filter(employee -> !employee.getAvailabilityDate().isBefore(start))
                .filter(employee -> !employee.getAvailabilityDate().isAfter(end))
                .sorted(Comparator
                        .comparing(InternalEmployee::getAvailabilityDate)
                        .thenComparing(InternalEmployee::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::mapToBenchEmployeeDto)
                .collect(Collectors.toList());
    }

    private BenchEmployeeDto mapToBenchEmployeeDto(InternalEmployee employee) {
        BenchEmployeeDto dto = new BenchEmployeeDto();
        dto.setEmployeeId(employee.getId());
        dto.setEmployeeCode(employee.getEmployeeId());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setLevel(employee.getLevel());
        dto.setAvailabilityDate(employee.getAvailabilityDate());
        dto.setLocation(employee.getLocation());
        dto.setContractType(employee.getContractType());
        dto.setCurrentProject(employee.getCurrentProject());
        dto.setUtilisationPct(employee.getUtilisationPct());
        dto.setManagerId(employee.getManagerId());
        dto.setHrisSyncStatus(employee.getHrisSyncStatus());
        dto.setSkills(employee.getSkills() != null ? Arrays.asList(employee.getSkills()) : null);
        return dto;
    }
}
