package com.talentgrid.workforce.rmgdashboard.service.impl;

import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSearchRequest;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSearchResponse;
import com.talentgrid.workforce.rmgdashboard.dto.SkillDto;
import com.talentgrid.workforce.rmgdashboard.service.RmgService;
import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemandSearchServiceImpl")
class DemandSearchServiceImplTest {

    @Mock
    private RmgService rmgService;

    private DemandSearchServiceImpl service;

    private DemandDto javaDemand;
    private DemandDto pythonDemand;

    @BeforeEach
    void setUp() {
        service = new DemandSearchServiceImpl(rmgService);

        javaDemand = DemandDto.builder()
                .demandId(1L)
                .title("Senior Java Developer")
                .location("Bangalore")
                .level("SENIOR")
                .status("INTERNAL_SEARCH")
                .priority("HIGH")
                .accountName("Acme Corp")
                .businessUnit("Engineering")
                .employmentType("FULL_TIME")
                .assignedRm(10L)
                .targetDate(LocalDate.of(2026, 7, 1))
                .mandatorySkills(List.of(new SkillDto(1L, "Java")))
                .build();

        pythonDemand = DemandDto.builder()
                .demandId(2L)
                .title("Python Data Engineer")
                .location("London")
                .level("MID")
                .status("APPROVED")
                .priority("MEDIUM")
                .accountName("Globex")
                .businessUnit("Data")
                .employmentType("CONTRACT")
                .assignedRm(20L)
                .targetDate(LocalDate.of(2026, 8, 15))
                .mandatorySkills(List.of(new SkillDto(2L, "Python")))
                .build();

        when(rmgService.getDemandsByStatuses(any(), eq(PageRequest.of(0, 500))))
                .thenReturn(new PageImpl<>(List.of(javaDemand, pythonDemand)));
    }

    @Test
    @DisplayName("returns all demands when no filters are provided")
    void noFiltersReturnsAll() {
        DemandSearchResponse response = service.search(new DemandSearchRequest());

        assertThat(response.getTotalResults()).isEqualTo(2);
        assertThat(response.getResults()).extracting(DemandDto::getDemandId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("filters by demandId")
    void filtersByDemandId() {
        DemandSearchRequest request = new DemandSearchRequest();
        request.setDemandId(2L);

        DemandSearchResponse response = service.search(request);

        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getResults().get(0).getDemandId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("filters by skill (case-insensitive exact match)")
    void filtersBySkill() {
        DemandSearchRequest request = new DemandSearchRequest();
        request.setSkill("java");

        DemandSearchResponse response = service.search(request);

        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getResults().get(0).getDemandId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("filters by title substring")
    void filtersByTitle() {
        DemandSearchRequest request = new DemandSearchRequest();
        request.setTitle("python");

        DemandSearchResponse response = service.search(request);

        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getResults().get(0).getTitle()).contains("Python");
    }

    @Test
    @DisplayName("filters by location, level, priority, and assignedRm together")
    void filtersByMultipleCriteria() {
        DemandSearchRequest request = new DemandSearchRequest();
        request.setLocation("bang");
        request.setLevel(Level.SENIOR);
        request.setPriority("HIGH");
        request.setAssignedRm(10L);
        request.setEmploymentType(ContractType.FULL_TIME);

        DemandSearchResponse response = service.search(request);

        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getResults().get(0).getDemandId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("filters by target date range")
    void filtersByTargetDateRange() {
        DemandSearchRequest request = new DemandSearchRequest();
        request.setTargetDateFrom(LocalDate.of(2026, 8, 1));
        request.setTargetDateTo(LocalDate.of(2026, 9, 1));

        DemandSearchResponse response = service.search(request);

        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getResults().get(0).getDemandId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("sorts by targetDate ascending then priority")
    void sortsResults() {
        DemandSearchResponse response = service.search(new DemandSearchRequest());

        assertThat(response.getResults()).extracting(DemandDto::getDemandId).containsExactly(1L, 2L);
    }
}
