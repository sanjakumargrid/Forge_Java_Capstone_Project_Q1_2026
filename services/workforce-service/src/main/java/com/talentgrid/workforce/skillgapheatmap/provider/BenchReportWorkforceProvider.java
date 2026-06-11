package com.talentgrid.workforce.skillgapheatmap.provider;

import com.talentgrid.workforce.benchreport.dto.BenchEmployeeDto;
import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;
import com.talentgrid.workforce.benchreport.service.BenchReportService;
import com.talentgrid.workforce.skillgapheatmap.provider.model.EngineerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * WorkforceProvider that obtains bench engineers directly from the in-process
 * BenchReportService bean. This avoids an HTTP round-trip to this service's own
 * /api/v1/bench-report endpoint (both live in workforce-service), removing the
 * network hop, the JSON (de)serialization, and the startup ordering race.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BenchReportWorkforceProvider implements WorkforceProvider {

    private final BenchReportService benchReportService;

    @Override
    public List<EngineerResponse> getBenchEngineers() {

        BenchReportResponse report = benchReportService.getBenchReport();

        if (report == null) {
            log.warn("[SKILL-GAP] Bench report returned null — returning empty engineer list.");
            return Collections.emptyList();
        }

        // Collect all engineers across all three availability windows
        List<BenchEmployeeDto> allEmployees = new ArrayList<>();

        if (report.getUnder30Days() != null) {
            allEmployees.addAll(report.getUnder30Days());
        }
        if (report.getThirtyToSixtyDays() != null) {
            allEmployees.addAll(report.getThirtyToSixtyDays());
        }
        if (report.getSixtyToNinetyDays() != null) {
            allEmployees.addAll(report.getSixtyToNinetyDays());
        }

        return allEmployees.stream()
                .filter(Objects::nonNull)
                .map(this::mapToEngineerResponse)
                .toList();
    }

    private EngineerResponse mapToEngineerResponse(BenchEmployeeDto employee) {
        return EngineerResponse.builder()
                .employeeId(employee.getEmployeeId())
                .name(employee.getName())
                .skills(employee.getSkills() != null ? employee.getSkills() : Collections.emptyList())
                .build();
    }
}
