package com.talentgrid.workforce.engineerprofilemanagement.service.impl;

import com.talentgrid.workforce.engineerprofilemanagement.dto.WorkforceAnalyticsResponse;
import com.talentgrid.workforce.engineerprofilemanagement.repository.InternalEmployeeRepository;
import com.talentgrid.workforce.engineerprofilemanagement.repository.SkillCatalogRepository;
import com.talentgrid.workforce.engineerprofilemanagement.service.SkillCatalogValidator;
import com.talentgrid.workforce.kafka.producer.WorkforceKafkaProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalEmployeeServiceImplAnalyticsTest {

    @Mock
    private InternalEmployeeRepository repository;

    @Mock
    private SkillCatalogRepository skillCatalogRepository;

    @Mock
    private SkillCatalogValidator skillCatalogValidator;

    @Mock
    private WorkforceKafkaProducer workforceKafkaProducer;

    @InjectMocks
    private InternalEmployeeServiceImpl internalEmployeeService;

    @Test
    void returnsWorkforceAnalyticsCountsByAvailabilityWindow() {
        LocalDate today = LocalDate.now();

        when(repository.countByIsDeletedFalse()).thenReturn(120L);
        when(repository.countByAvailabilityDateBetweenAndIsDeletedFalse(eq(today), eq(today.plusDays(30))))
                .thenReturn(25L);
        when(repository.countByAvailabilityDateBetweenAndIsDeletedFalse(eq(today.plusDays(31)), eq(today.plusDays(60))))
                .thenReturn(18L);
        when(repository.countByAvailabilityDateBetweenAndIsDeletedFalse(eq(today.plusDays(61)), eq(today.plusDays(90))))
                .thenReturn(12L);

        WorkforceAnalyticsResponse response = internalEmployeeService.getWorkforceAnalytics();

        assertEquals(120L, response.getTotalWorkforce());
        assertEquals(25L, response.getUnder30Days());
        assertEquals(18L, response.getThirtyToSixtyDays());
        assertEquals(12L, response.getSixtyToNinetyDays());
    }
}
