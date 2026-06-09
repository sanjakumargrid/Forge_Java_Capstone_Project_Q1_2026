package com.talentgrid.workforce.benchreport.service.impl;

import com.talentgrid.workforce.benchreport.dto.BenchEmployeeDto;
import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;
import com.talentgrid.workforce.benchreport.repository.BenchReportRepository;
import com.talentgrid.workforce.benchreport.service.BenchReportService;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BenchReportServiceImpl implements BenchReportService {

    private final BenchReportRepository benchReportRepository;
    private final AtomicReference<BenchReportResponse> cachedReport = new AtomicReference<>(new BenchReportResponse());

    public BenchReportServiceImpl(BenchReportRepository benchReportRepository) {
        this.benchReportRepository = benchReportRepository;
    }

    @PostConstruct
    public void init() {
        refreshBenchReport();
    }

    @Override
    public BenchReportResponse getBenchReport() {
        return cachedReport.get();
    }

    @Override
    @Scheduled(cron = "0 0 6 * * *", zone = "UTC")
    public void refreshBenchReport() {
        LocalDate today = LocalDate.now();
        LocalDate ninetyDays = today.plusDays(90);

        List<InternalEmployee> employees = benchReportRepository
                .findByAvailabilityDateBetweenAndIsDeletedFalseOrderByAvailabilityDateAsc(today, ninetyDays);

        BenchReportResponse response = new BenchReportResponse();
        response.setRefreshedAt(Instant.now());

        response.setUnder30Days(groupEmployeesWithinRange(employees, today, today.plusDays(30)));
        response.setThirtyToSixtyDays(groupEmployeesWithinRange(employees, today.plusDays(31), today.plusDays(60)));
        response.setSixtyToNinetyDays(groupEmployeesWithinRange(employees, today.plusDays(61), ninetyDays));

        cachedReport.set(response);
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
                .map(this::mapToBenchEmployeeDto)
                .collect(Collectors.toList());
    }

    private BenchEmployeeDto mapToBenchEmployeeDto(InternalEmployee employee) {
        BenchEmployeeDto dto = new BenchEmployeeDto();
        dto.setEmployeeId(employee.getEmployeeId());
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
        dto.setSkills(employee.getSkills());
        return dto;
    }
}
