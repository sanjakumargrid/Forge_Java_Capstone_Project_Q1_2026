package com.talentgrid.workforce.skillgapheatmap.provider;

import com.talentgrid.workforce.benchreport.repository.BenchReportRepository;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import com.talentgrid.workforce.skillgapheatmap.provider.model.EngineerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * WorkforceProvider that reads bench engineers directly from the database so
 * skill-gap snapshots always reflect the latest skills, without relying on the
 * daily bench-report cache.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BenchReportWorkforceProvider implements WorkforceProvider {

    private final BenchReportRepository benchReportRepository;

    @Override
    public List<EngineerResponse> getBenchEngineers() {
        LocalDate today = LocalDate.now();
        LocalDate ninetyDaysAhead = today.plusDays(90);

        List<InternalEmployee> employees = benchReportRepository
                .findByAvailabilityDateBetweenAndIsDeletedFalseOrderByAvailabilityDateAsc(
                        today, ninetyDaysAhead);

        if (employees.isEmpty()) {
            log.debug("[SKILL-GAP] No bench engineers found for availability window {} to {}.",
                    today, ninetyDaysAhead);
            return Collections.emptyList();
        }

        return employees.stream()
                .filter(Objects::nonNull)
                .map(this::mapToEngineerResponse)
                .toList();
    }

    private EngineerResponse mapToEngineerResponse(InternalEmployee employee) {
        return EngineerResponse.builder()
                .employeeId(employee.getId())
                .name(employee.getName())
                .skills(employee.getSkills() != null
                        ? Arrays.asList(employee.getSkills())
                        : Collections.emptyList())
                .build();
    }
}
