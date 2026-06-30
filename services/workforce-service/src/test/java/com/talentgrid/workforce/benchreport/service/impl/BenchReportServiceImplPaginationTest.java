package com.talentgrid.workforce.benchreport.service.impl;

import com.talentgrid.workforce.benchreport.dto.BenchReportPageResponse;
import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;
import com.talentgrid.workforce.benchreport.repository.BenchReportRepository;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenchReportServiceImplPaginationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 29);

    @Mock
    private BenchReportRepository benchReportRepository;

    private BenchReportServiceImpl benchReportService;

    @BeforeEach
    void setUp() {
        benchReportService = new BenchReportServiceImpl(benchReportRepository);
    }

    @Test
    void under30DaysIncludesPastAvailabilityDates() {
        when(benchReportRepository.findByAvailabilityDateLessThanEqualAndIsDeletedFalseOrderByAvailabilityDateAsc(
                eq(TODAY.plusDays(30))))
                .thenReturn(List.of(
                        employee("Past Employee", TODAY.minusDays(10)),
                        employee("assha Doe", LocalDate.of(2026, 7, 8))
                ));
        when(benchReportRepository.findByAvailabilityDateBetweenAndIsDeletedFalseOrderByAvailabilityDateAsc(
                eq(TODAY.plusDays(31)), eq(TODAY.plusDays(60))))
                .thenReturn(List.of(employee("Kabir Kulkarni", LocalDate.of(2026, 8, 20))));
        when(benchReportRepository.findByAvailabilityDateBetweenAndIsDeletedFalseOrderByAvailabilityDateAsc(
                eq(TODAY.plusDays(61)), eq(TODAY.plusDays(90))))
                .thenReturn(List.of());

        BenchReportResponse response = benchReportService.buildBenchReport(TODAY);

        assertEquals(2, response.getUnder30Days().size());
        assertEquals("Past Employee", response.getUnder30Days().get(0).getName());
        assertEquals(1, response.getThirtyToSixtyDays().size());
        assertEquals(0, response.getSixtyToNinetyDays().size());
    }

    @Test
    void paginatesEachWindowIndependently() {
        List<InternalEmployee> under30 = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            under30.add(employee("Under30-" + i, TODAY.plusDays(10)));
        }
        List<InternalEmployee> thirtyToSixty = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            thirtyToSixty.add(employee("ThirtyToSixty-" + i, TODAY.plusDays(40)));
        }

        when(benchReportRepository.findByAvailabilityDateLessThanEqualAndIsDeletedFalseOrderByAvailabilityDateAsc(
                eq(LocalDate.now().plusDays(30))))
                .thenReturn(under30);
        when(benchReportRepository.findByAvailabilityDateBetweenAndIsDeletedFalseOrderByAvailabilityDateAsc(
                eq(LocalDate.now().plusDays(31)), eq(LocalDate.now().plusDays(60))))
                .thenReturn(thirtyToSixty);
        when(benchReportRepository.findByAvailabilityDateBetweenAndIsDeletedFalseOrderByAvailabilityDateAsc(
                eq(LocalDate.now().plusDays(61)), eq(LocalDate.now().plusDays(90))))
                .thenReturn(List.of());

        BenchReportPageResponse response = benchReportService.getBenchReport(0, 10);

        assertEquals(10, response.getUnder30Days().getContent().size());
        assertEquals(15, response.getUnder30Days().getTotalElements());
        assertEquals(2, response.getUnder30Days().getTotalPages());
        assertEquals(5, response.getThirtyToSixtyDays().getContent().size());
        assertEquals(0, response.getSixtyToNinetyDays().getContent().size());
    }

    @Test
    void rejectsInvalidPageOrSize() {
        assertThrows(IllegalArgumentException.class, () -> benchReportService.getBenchReport(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> benchReportService.getBenchReport(0, 0));
    }

    private InternalEmployee employee(String name, LocalDate availabilityDate) {
        InternalEmployee employee = new InternalEmployee();
        employee.setId((long) name.hashCode());
        employee.setEmployeeId((long) name.hashCode());
        employee.setName(name);
        employee.setAvailabilityDate(availabilityDate);
        employee.setIsDeleted(false);
        return employee;
    }
}
