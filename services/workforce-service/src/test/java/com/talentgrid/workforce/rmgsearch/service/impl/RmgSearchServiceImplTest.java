package com.talentgrid.workforce.rmgsearch.service.impl;

import com.talentgrid.workforce.benchreport.dto.BenchEmployeeDto;
import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;
import com.talentgrid.workforce.benchreport.service.BenchReportService;
import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import com.talentgrid.workforce.rmgsearch.dto.RmgSearchRequest;
import com.talentgrid.workforce.rmgsearch.dto.RmgSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RmgSearchServiceImpl")
class RmgSearchServiceImplTest {

    @Mock
    private BenchReportService benchReportService;

    @InjectMocks
    private RmgSearchServiceImpl rmgSearchService;

    // Four bench employees spread across all three availability buckets
    private BenchEmployeeDto alice;   // SENIOR, FULL_TIME,  New York, +5d,  Java + Spring
    private BenchEmployeeDto dan;     // SENIOR, PART_TIME,  Berlin,   +10d, Java
    private BenchEmployeeDto bob;     // MID,    CONTRACT,   London,   +35d, Python + Django
    private BenchEmployeeDto carol;   // JUNIOR, FULL_TIME,  New York, +65d, Java + React

    @BeforeEach
    void setUp() {
        alice = buildEmployee(1L, "Alice", Level.SENIOR, ContractType.FULL_TIME,
                "New York", LocalDate.now().plusDays(5), "Java", "Spring");
        dan   = buildEmployee(4L, "Dan",   Level.SENIOR, ContractType.PART_TIME,
                "Berlin",   LocalDate.now().plusDays(10), "Java");
        bob   = buildEmployee(2L, "Bob",   Level.MID,    ContractType.CONTRACT,
                "London",   LocalDate.now().plusDays(35), "Python", "Django");
        carol = buildEmployee(3L, "Carol", Level.JUNIOR, ContractType.FULL_TIME,
                "New York",  LocalDate.now().plusDays(65), "Java", "React");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BenchEmployeeDto buildEmployee(Long id, String name, Level level,
                                           ContractType contractType, String location,
                                           LocalDate availabilityDate, String... skills) {
        BenchEmployeeDto dto = new BenchEmployeeDto();
        dto.setEmployeeId(id);
        dto.setName(name);
        dto.setLevel(level);
        dto.setContractType(contractType);
        dto.setLocation(location);
        dto.setAvailabilityDate(availabilityDate);
        dto.setSkills(List.of(skills));
        return dto;
    }

    private BenchReportResponse reportWith(List<BenchEmployeeDto> under30,
                                           List<BenchEmployeeDto> thirtyTo60,
                                           List<BenchEmployeeDto> sixtyTo90) {
        BenchReportResponse r = new BenchReportResponse();
        r.getUnder30Days().addAll(under30);
        r.getThirtyToSixtyDays().addAll(thirtyTo60);
        r.getSixtyToNinetyDays().addAll(sixtyTo90);
        return r;
    }

    private BenchReportResponse fullReport() {
        return reportWith(List.of(alice, dan), List.of(bob), List.of(carol));
    }

    // -------------------------------------------------------------------------
    // No filters — baseline behaviour
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("No filters")
    class NoFilters {

        @Test
        @DisplayName("returns all employees from all three buckets")
        void returnsAllEmployeesFromAllBuckets() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchResponse response = rmgSearchService.search(new RmgSearchRequest());

            assertThat(response.getTotalResults()).isEqualTo(4);
            assertThat(response.getResults())
                    .extracting(BenchEmployeeDto::getEmployeeId)
                    .containsExactly(1L, 4L, 2L, 3L);
        }

        @Test
        @DisplayName("returns empty list when bench report has no employees")
        void returnsEmptyWhenBenchReportIsEmpty() {
            when(benchReportService.getBenchReport()).thenReturn(new BenchReportResponse());

            RmgSearchResponse response = rmgSearchService.search(new RmgSearchRequest());

            assertThat(response.getTotalResults()).isEqualTo(0);
            assertThat(response.getResults()).isEmpty();
        }

        @Test
        @DisplayName("response always contains a queriedAt timestamp")
        void responseContainsQueriedAtTimestamp() {
            when(benchReportService.getBenchReport()).thenReturn(new BenchReportResponse());

            assertThat(rmgSearchService.search(new RmgSearchRequest()).getQueriedAt()).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // Skill filter
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Skill filter")
    class SkillFilter {

        @Test
        @DisplayName("returns only employees who have the requested skill")
        void returnsEmployeesWithMatchingSkill() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setSkill("Java");

            RmgSearchResponse response = rmgSearchService.search(request);

            assertThat(response.getResults())
                    .extracting(BenchEmployeeDto::getEmployeeId)
                    .containsExactlyInAnyOrder(1L, 4L, 3L);
        }

        @Test
        @DisplayName("skill match is case-insensitive")
        void skillMatchIsCaseInsensitive() {
            when(benchReportService.getBenchReport())
                    .thenReturn(reportWith(List.of(alice), List.of(), List.of()));

            RmgSearchRequest request = new RmgSearchRequest();
            request.setSkill("JAVA");

            assertThat(rmgSearchService.search(request).getTotalResults()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns empty when no employee has the requested skill")
        void returnsEmptyWhenNoSkillMatch() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setSkill("Rust");

            assertThat(rmgSearchService.search(request).getTotalResults()).isEqualTo(0);
        }

        @Test
        @DisplayName("excludes employees whose skills list is null")
        void excludesEmployeesWithNullSkillsList() {
            BenchEmployeeDto noSkills = buildEmployee(99L, "Eve", Level.MID,
                    ContractType.FULL_TIME, "Oslo", LocalDate.now().plusDays(20));
            noSkills.setSkills(null);

            when(benchReportService.getBenchReport())
                    .thenReturn(reportWith(List.of(noSkills), List.of(), List.of()));

            RmgSearchRequest request = new RmgSearchRequest();
            request.setSkill("Java");

            assertThat(rmgSearchService.search(request).getTotalResults()).isEqualTo(0);
        }

        @Test
        @DisplayName("excludes employees whose skills list is empty")
        void excludesEmployeesWithEmptySkillsList() {
            BenchEmployeeDto emptySkills = buildEmployee(98L, "Fay", Level.MID,
                    ContractType.FULL_TIME, "Oslo", LocalDate.now().plusDays(20));
            emptySkills.setSkills(List.of());

            when(benchReportService.getBenchReport())
                    .thenReturn(reportWith(List.of(emptySkills), List.of(), List.of()));

            RmgSearchRequest request = new RmgSearchRequest();
            request.setSkill("Java");

            assertThat(rmgSearchService.search(request).getTotalResults()).isEqualTo(0);
        }

        @Test
        @DisplayName("blank skill string acts as no filter")
        void blankSkillActsAsNoFilter() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setSkill("   ");

            assertThat(rmgSearchService.search(request).getTotalResults()).isEqualTo(4);
        }
    }

    // -------------------------------------------------------------------------
    // Availability date filters
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Availability date filters")
    class AvailabilityDateFilters {

        @Test
        @DisplayName("availabilityDateFrom excludes employees available earlier than the bound")
        void dateFromExcludesEarlierDates() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setAvailabilityDateFrom(LocalDate.now().plusDays(11));

            RmgSearchResponse response = rmgSearchService.search(request);

            assertThat(response.getResults())
                    .extracting(BenchEmployeeDto::getEmployeeId)
                    .doesNotContain(1L, 4L)
                    .contains(2L, 3L);
        }

        @Test
        @DisplayName("availabilityDateTo excludes employees available later than the bound")
        void dateToExcludesLaterDates() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setAvailabilityDateTo(LocalDate.now().plusDays(30));

            RmgSearchResponse response = rmgSearchService.search(request);

            assertThat(response.getResults())
                    .extracting(BenchEmployeeDto::getEmployeeId)
                    .containsExactlyInAnyOrder(1L, 4L);
        }

        @Test
        @DisplayName("availabilityDateFrom and availabilityDateTo together define an inclusive range")
        void dateRangeIsInclusive() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setAvailabilityDateFrom(LocalDate.now().plusDays(6));
            request.setAvailabilityDateTo(LocalDate.now().plusDays(40));

            RmgSearchResponse response = rmgSearchService.search(request);

            // alice (+5d) is excluded, dan (+10d) and bob (+35d) are included, carol (+65d) is excluded
            assertThat(response.getResults())
                    .extracting(BenchEmployeeDto::getEmployeeId)
                    .containsExactly(4L, 2L);
        }
    }

    // -------------------------------------------------------------------------
    // Location filter
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Location filter")
    class LocationFilter {

        @Test
        @DisplayName("matches are case-insensitive substrings")
        void caseInsensitiveSubstringMatch() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setLocation("new york");

            assertThat(rmgSearchService.search(request).getResults())
                    .extracting(BenchEmployeeDto::getEmployeeId)
                    .containsExactlyInAnyOrder(1L, 3L);
        }

        @Test
        @DisplayName("returns empty when no employee is in the given location")
        void returnsEmptyWhenNoLocationMatch() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setLocation("Tokyo");

            assertThat(rmgSearchService.search(request).getTotalResults()).isEqualTo(0);
        }

        @Test
        @DisplayName("blank location string acts as no filter")
        void blankLocationActsAsNoFilter() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setLocation("  ");

            assertThat(rmgSearchService.search(request).getTotalResults()).isEqualTo(4);
        }
    }

    // -------------------------------------------------------------------------
    // Seniority filter
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Seniority filter")
    class SeniorityFilter {

        @Test
        @DisplayName("returns only employees at the requested seniority level")
        void exactSeniorityMatch() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setSeniority(Level.SENIOR);

            assertThat(rmgSearchService.search(request).getResults())
                    .extracting(BenchEmployeeDto::getEmployeeId)
                    .containsExactlyInAnyOrder(1L, 4L);
        }

        @Test
        @DisplayName("null seniority acts as no filter")
        void nullSeniorityActsAsNoFilter() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            assertThat(rmgSearchService.search(new RmgSearchRequest()).getTotalResults()).isEqualTo(4);
        }
    }

    // -------------------------------------------------------------------------
    // ContractType filter
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ContractType filter")
    class ContractTypeFilter {

        @Test
        @DisplayName("returns only employees with the requested contract type")
        void exactContractTypeMatch() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setContractType(ContractType.FULL_TIME);

            assertThat(rmgSearchService.search(request).getResults())
                    .extracting(BenchEmployeeDto::getEmployeeId)
                    .containsExactlyInAnyOrder(1L, 3L);
        }

        @Test
        @DisplayName("null contractType acts as no filter")
        void nullContractTypeActsAsNoFilter() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            assertThat(rmgSearchService.search(new RmgSearchRequest()).getTotalResults()).isEqualTo(4);
        }
    }

    // -------------------------------------------------------------------------
    // Multi-criteria AND logic
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Multi-criteria AND logic")
    class MultiCriteriaAnd {

        @Test
        @DisplayName("all active filters must match simultaneously")
        void allFiltersMustMatchSimultaneously() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setSkill("Java");
            request.setSeniority(Level.SENIOR);
            request.setContractType(ContractType.FULL_TIME);
            request.setLocation("New York");

            RmgSearchResponse response = rmgSearchService.search(request);

            // Only Alice satisfies every criterion
            assertThat(response.getTotalResults()).isEqualTo(1);
            assertThat(response.getResults().get(0).getEmployeeId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("returns empty when no employee satisfies all active filters")
        void returnsEmptyWhenNoEmployeeSatisfiesAllFilters() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setSkill("Python");   // only Bob
            request.setSeniority(Level.SENIOR); // Bob is MID — contradiction

            assertThat(rmgSearchService.search(request).getTotalResults()).isEqualTo(0);
        }

        @Test
        @DisplayName("skill + location combination narrows results correctly")
        void skillAndLocationCombination() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            RmgSearchRequest request = new RmgSearchRequest();
            request.setSkill("Java");
            request.setLocation("New York");

            // alice (1L) and carol (3L) are Java engineers in New York; dan (4L) is in Berlin
            assertThat(rmgSearchService.search(request).getResults())
                    .extracting(BenchEmployeeDto::getEmployeeId)
                    .containsExactlyInAnyOrder(1L, 3L);
        }
    }

    // -------------------------------------------------------------------------
    // Sorting
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Sorting")
    class Sorting {

        @Test
        @DisplayName("results are ordered by availabilityDate ascending")
        void resultsSortedByAvailabilityDateAscending() {
            when(benchReportService.getBenchReport()).thenReturn(fullReport());

            List<LocalDate> dates = rmgSearchService.search(new RmgSearchRequest())
                    .getResults()
                    .stream()
                    .map(BenchEmployeeDto::getAvailabilityDate)
                    .toList();

            assertThat(dates).isSorted();
        }

        @Test
        @DisplayName("employees with null availabilityDate appear last")
        void nullAvailabilityDateSortedLast() {
            BenchEmployeeDto noDate = buildEmployee(99L, "Zara", Level.MID,
                    ContractType.FULL_TIME, "Oslo", null);
            noDate.setAvailabilityDate(null);

            when(benchReportService.getBenchReport())
                    .thenReturn(reportWith(List.of(alice, noDate), List.of(), List.of()));

            RmgSearchResponse response = rmgSearchService.search(new RmgSearchRequest());

            assertThat(response.getResults())
                    .last()
                    .extracting(BenchEmployeeDto::getEmployeeId)
                    .isEqualTo(99L);
        }

        @Test
        @DisplayName("sorting is stable across all three availability buckets")
        void sortingIsStableAcrossBuckets() {
            // Deliberately put employees in non-chronological bucket order
            when(benchReportService.getBenchReport())
                    .thenReturn(reportWith(List.of(dan), List.of(alice), List.of(carol, bob)));

            // Expected ascending order: alice(+5) → dan(+10) → bob(+35) → carol(+65)
            // even though alice was placed in the 30–60 bucket above
            assertThat(rmgSearchService.search(new RmgSearchRequest()).getResults())
                    .extracting(BenchEmployeeDto::getEmployeeId)
                    .containsExactly(1L, 4L, 2L, 3L);
        }
    }
}
