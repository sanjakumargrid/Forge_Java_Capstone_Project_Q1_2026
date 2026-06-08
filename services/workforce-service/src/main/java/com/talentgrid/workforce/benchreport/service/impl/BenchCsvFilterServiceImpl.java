package com.talentgrid.workforce.benchreport.service.impl;

import com.talentgrid.workforce.benchreport.dto.BenchFilterPageResponse;
import com.talentgrid.workforce.benchreport.dto.BenchFilterRequest;
import com.talentgrid.workforce.benchreport.service.BenchCsvFilterService;
import com.talentgrid.workforce.benchreport.dto.BenchEmployeeDto;
import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;
import com.talentgrid.workforce.benchreport.service.BenchReportService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BenchCsvFilterServiceImpl implements BenchCsvFilterService {

    private final BenchReportService benchReportService;

    public BenchCsvFilterServiceImpl(BenchReportService benchReportService) {
        this.benchReportService = benchReportService;
    }

    @Override
    public BenchFilterPageResponse searchBenchEmployees(BenchFilterRequest request, int page, int size, String sortBy, String sortDirection) {
        List<BenchEmployeeDto> filtered = getFilteredAndSortedEmployees(request, sortBy, sortDirection);
        
        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        int start = Math.min(page * size, totalElements);
        int end = Math.min(start + size, totalElements);
        List<BenchEmployeeDto> content = filtered.subList(start, end);
        
        return new BenchFilterPageResponse(content, page, size, totalElements, totalPages, sortBy, sortDirection);
    }

    @Override
    public byte[] exportBenchEmployeesCsv(BenchFilterRequest request, String sortBy, String sortDirection) {
        List<BenchEmployeeDto> filtered = getFilteredAndSortedEmployees(request, sortBy, sortDirection);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            // Header
            writer.println("Employee ID,Name,Email,Level,Availability Date,Location,Contract Type,Current Project,Utilisation %,Manager ID,HRIS Sync Status");
            
            // Data
            for (BenchEmployeeDto emp : filtered) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        escapeCsv(emp.getEmployeeId()),
                        escapeCsv(emp.getName()),
                        escapeCsv(emp.getEmail()),
                        emp.getLevel(),
                        emp.getAvailabilityDate(),
                        escapeCsv(emp.getLocation()),
                        emp.getContractType(),
                        escapeCsv(emp.getCurrentProject()),
                        emp.getUtilisationPct(),
                        emp.getManagerId(),
                        emp.getHrisSyncStatus());
            }
            writer.flush();
        }
        return out.toByteArray();
    }
    
    private String escapeCsv(String data) {
        if (data == null) {
            return "";
        }
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    private List<BenchEmployeeDto> getFilteredAndSortedEmployees(BenchFilterRequest request, String sortBy, String sortDirection) {
        BenchReportResponse report = benchReportService.getBenchReport();
        
        List<BenchEmployeeDto> allEmployees = new ArrayList<>();
        if (report.getUnder30Days() != null) allEmployees.addAll(report.getUnder30Days());
        if (report.getThirtyToSixtyDays() != null) allEmployees.addAll(report.getThirtyToSixtyDays());
        if (report.getSixtyToNinetyDays() != null) allEmployees.addAll(report.getSixtyToNinetyDays());
        
        Stream<BenchEmployeeDto> stream = allEmployees.stream();
        
        if (request != null) {
            if (StringUtils.hasText(request.getEmployeeId())) {
                stream = stream.filter(e -> e.getEmployeeId() != null && e.getEmployeeId().toLowerCase().contains(request.getEmployeeId().toLowerCase()));
            }
            if (StringUtils.hasText(request.getName())) {
                stream = stream.filter(e -> e.getName() != null && e.getName().toLowerCase().contains(request.getName().toLowerCase()));
            }
            if (StringUtils.hasText(request.getEmail())) {
                stream = stream.filter(e -> e.getEmail() != null && e.getEmail().toLowerCase().contains(request.getEmail().toLowerCase()));
            }
            if (request.getLevel() != null) {
                stream = stream.filter(e -> e.getLevel() == request.getLevel());
            }
            if (StringUtils.hasText(request.getLocation())) {
                stream = stream.filter(e -> e.getLocation() != null && e.getLocation().toLowerCase().contains(request.getLocation().toLowerCase()));
            }
            if (request.getContractType() != null) {
                stream = stream.filter(e -> e.getContractType() == request.getContractType());
            }
            if (StringUtils.hasText(request.getCurrentProject())) {
                stream = stream.filter(e -> e.getCurrentProject() != null && e.getCurrentProject().toLowerCase().contains(request.getCurrentProject().toLowerCase()));
            }
            if (request.getHrisSyncStatus() != null) {
                stream = stream.filter(e -> e.getHrisSyncStatus() == request.getHrisSyncStatus());
            }
            if (request.getAvailabilityDateFrom() != null) {
                stream = stream.filter(e -> e.getAvailabilityDate() != null && !e.getAvailabilityDate().isBefore(request.getAvailabilityDateFrom()));
            }
            if (request.getAvailabilityDateTo() != null) {
                stream = stream.filter(e -> e.getAvailabilityDate() != null && !e.getAvailabilityDate().isAfter(request.getAvailabilityDateTo()));
            }
            if (request.getUtilisationPctMin() != null) {
                stream = stream.filter(e -> e.getUtilisationPct() != null && e.getUtilisationPct() >= request.getUtilisationPctMin());
            }
            if (request.getUtilisationPctMax() != null) {
                stream = stream.filter(e -> e.getUtilisationPct() != null && e.getUtilisationPct() <= request.getUtilisationPctMax());
            }
            if (request.getManagerId() != null) {
                stream = stream.filter(e -> request.getManagerId().equals(e.getManagerId()));
            }
            if (StringUtils.hasText(request.getDayFilter())) {
                LocalDate today = LocalDate.now();
                String filter = request.getDayFilter().toLowerCase();
                if (filter.contains("under30")) {
                    stream = stream.filter(e -> e.getAvailabilityDate() != null &&
                            !e.getAvailabilityDate().isBefore(today) &&
                            !e.getAvailabilityDate().isAfter(today.plusDays(30)));
                } else if (filter.contains("thirtytosixty") || filter.contains("30to60")) {
                    stream = stream.filter(e -> e.getAvailabilityDate() != null &&
                            !e.getAvailabilityDate().isBefore(today.plusDays(31)) &&
                            !e.getAvailabilityDate().isAfter(today.plusDays(60)));
                } else if (filter.contains("sixtytoninety") || filter.contains("60to90")) {
                    stream = stream.filter(e -> e.getAvailabilityDate() != null &&
                            !e.getAvailabilityDate().isBefore(today.plusDays(61)) &&
                            !e.getAvailabilityDate().isAfter(today.plusDays(90)));
                }
            }
        }
        
        Comparator<BenchEmployeeDto> comparator = getComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }
        
        return stream.sorted(comparator).collect(Collectors.toList());
    }
    
    private Comparator<BenchEmployeeDto> getComparator(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return Comparator.comparing(BenchEmployeeDto::getAvailabilityDate, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        
        return switch (sortBy) {
            case "employeeId" -> Comparator.comparing(BenchEmployeeDto::getEmployeeId, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "name" -> Comparator.comparing(BenchEmployeeDto::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "email" -> Comparator.comparing(BenchEmployeeDto::getEmail, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "level" -> Comparator.comparing(e -> e.getLevel() != null ? e.getLevel().name() : "", Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "location" -> Comparator.comparing(BenchEmployeeDto::getLocation, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "contractType" -> Comparator.comparing(e -> e.getContractType() != null ? e.getContractType().name() : "", Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "currentProject" -> Comparator.comparing(BenchEmployeeDto::getCurrentProject, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "utilisationPct" -> Comparator.comparing(BenchEmployeeDto::getUtilisationPct, Comparator.nullsLast(Comparator.naturalOrder()));
            case "managerId" -> Comparator.comparing(BenchEmployeeDto::getManagerId, Comparator.nullsLast(Comparator.naturalOrder()));
            case "hrisSyncStatus" -> Comparator.comparing(e -> e.getHrisSyncStatus() != null ? e.getHrisSyncStatus().name() : "", Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparing(BenchEmployeeDto::getAvailabilityDate, Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }
}
