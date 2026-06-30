package com.talentgrid.workforce.benchreport.service.impl;

import com.talentgrid.workforce.benchreport.dto.BenchEmployeeDto;
import com.talentgrid.workforce.benchreport.dto.BenchReportPageResponse;
import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BenchReportServiceImplPaginationTest {

    private BenchReportServiceImpl benchReportService;

    @BeforeEach
    void setUp() throws Exception {
        benchReportService = new BenchReportServiceImpl(null);

        BenchReportResponse cached = new BenchReportResponse();
        cached.setRefreshedAt(Instant.parse("2026-06-29T06:00:00Z"));
        cached.setUnder30Days(buildEmployees(15));
        cached.setThirtyToSixtyDays(buildEmployees(5));
        cached.setSixtyToNinetyDays(List.of());

        var cachedReportField = BenchReportServiceImpl.class.getDeclaredField("cachedReport");
        cachedReportField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var ref = (java.util.concurrent.atomic.AtomicReference<BenchReportResponse>) cachedReportField.get(benchReportService);
        ref.set(cached);
    }

    @Test
    void returnsFullReportWithoutPaginationParams() {
        BenchReportResponse response = benchReportService.getBenchReport();

        assertEquals(15, response.getUnder30Days().size());
        assertEquals(5, response.getThirtyToSixtyDays().size());
        assertEquals(0, response.getSixtyToNinetyDays().size());
    }

    @Test
    void returnsFirstPageWithDefaultSizeTenPerWindow() {
        BenchReportPageResponse response = benchReportService.getBenchReport(0, 10);

        assertEquals(10, response.getUnder30Days().getContent().size());
        assertEquals(15, response.getUnder30Days().getTotalElements());
        assertEquals(2, response.getUnder30Days().getTotalPages());
        assertEquals(0, response.getUnder30Days().getPage());
        assertEquals(10, response.getUnder30Days().getSize());

        assertEquals(5, response.getThirtyToSixtyDays().getContent().size());
        assertEquals(0, response.getSixtyToNinetyDays().getContent().size());
    }

    @Test
    void returnsSecondPageForLargeWindow() {
        BenchReportPageResponse response = benchReportService.getBenchReport(1, 10);

        assertEquals(5, response.getUnder30Days().getContent().size());
        assertEquals(1, response.getUnder30Days().getPage());
    }

    @Test
    void rejectsInvalidPageOrSize() {
        assertThrows(IllegalArgumentException.class, () -> benchReportService.getBenchReport(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> benchReportService.getBenchReport(0, 0));
    }

    private List<BenchEmployeeDto> buildEmployees(int count) {
        List<BenchEmployeeDto> employees = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BenchEmployeeDto dto = new BenchEmployeeDto();
            dto.setEmployeeId((long) i + 1);
            dto.setName("Employee " + (i + 1));
            employees.add(dto);
        }
        return employees;
    }
}
