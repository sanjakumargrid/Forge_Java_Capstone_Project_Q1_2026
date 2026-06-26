package com.talentgrid.demand.service;

import com.talentgrid.demand.dto.response.DemandAnalyticsMetricsResponse;
import com.talentgrid.demand.dto.response.DemandAnalyticsMetricsResponse.*;
import com.talentgrid.demand.repository.DemandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DemandAnalyticsService} — demand-level (single-person) model.
 * Each demand = one person, so fill rate = filled demands / total demands.
 */
class DemandAnalyticsServiceTest {

    @Mock
    private DemandRepository demandRepository;

    @InjectMocks
    private DemandAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAnalyticsWithDefaultWindow() {
        // Arrange
        when(demandRepository.countTotalDemandsBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(100L);
        when(demandRepository.countFilledDemandsBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(80L);
        when(demandRepository.countFilledDemandsInternalBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(55L);
        when(demandRepository.countFilledDemandsExternalBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(25L);
        when(demandRepository.averageTimeToFillBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(26.45);
        when(demandRepository.minTimeToFillBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(3.5);
        when(demandRepository.maxTimeToFillBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(120.0);
        when(demandRepository.countDemandsWithFilledPositionsBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(80L);

        // Act
        DemandAnalyticsMetricsResponse response = analyticsService.getAnalytics(null, null);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getMetadata());
        assertEquals(LocalDate.now(), response.getMetadata().getEndDate());

        // Verify fill rate — 80 filled out of 100 total = 80%
        assertNotNull(response.getFillRate());
        assertEquals(100L, response.getFillRate().getTotalRequiredPositions()); // total demands
        assertEquals(80L, response.getFillRate().getTotalFilledPositions());    // filled demands
        assertEquals(80.0, response.getFillRate().getFillRatePercent());

        // Verify internal vs external split (55 internal, 25 external → 68.75% / 31.25%)
        assertNotNull(response.getInternalVsExternalSplit());
        assertEquals(55L, response.getInternalVsExternalSplit().getInternalFilledCount());
        assertEquals(25L, response.getInternalVsExternalSplit().getExternalFilledCount());
        assertEquals(68.75, response.getInternalVsExternalSplit().getInternalPercentage());
        assertEquals(31.25, response.getInternalVsExternalSplit().getExternalPercentage());

        // Verify time-to-fill
        assertNotNull(response.getTimeToFill());
        assertEquals(26.45, response.getTimeToFill().getAverageDaysToFill());
        assertEquals(3.5, response.getTimeToFill().getMinDaysToFill());
        assertEquals(120.0, response.getTimeToFill().getMaxDaysToFill());
        assertEquals(80L, response.getTimeToFill().getTotalFilledDemands());
    }

    @Test
    void testGetAnalyticsWithCustomDateRange() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 5, 15);
        LocalDate endDate = LocalDate.of(2026, 6, 14);

        when(demandRepository.countTotalDemandsBetween(startDate, endDate)).thenReturn(50L);
        when(demandRepository.countFilledDemandsBetween(startDate, endDate)).thenReturn(40L);
        when(demandRepository.countFilledDemandsInternalBetween(startDate, endDate)).thenReturn(28L);
        when(demandRepository.countFilledDemandsExternalBetween(startDate, endDate)).thenReturn(12L);
        when(demandRepository.averageTimeToFillBetween(startDate, endDate)).thenReturn(20.0);
        when(demandRepository.minTimeToFillBetween(startDate, endDate)).thenReturn(2.0);
        when(demandRepository.maxTimeToFillBetween(startDate, endDate)).thenReturn(45.0);
        when(demandRepository.countDemandsWithFilledPositionsBetween(startDate, endDate)).thenReturn(40L);

        // Act
        DemandAnalyticsMetricsResponse response = analyticsService.getAnalytics(startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(startDate, response.getMetadata().getStartDate());
        assertEquals(endDate, response.getMetadata().getEndDate());
        assertEquals(31, response.getMetadata().getWindowDays()); // 31 days inclusive

        assertEquals(80.0, response.getFillRate().getFillRatePercent()); // 40/50 = 80%
    }

    @Test
    void testGetAnalyticsWithZeroFilledDemands() {
        // Arrange
        when(demandRepository.countTotalDemandsBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(10L);
        when(demandRepository.countFilledDemandsBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);
        when(demandRepository.countFilledDemandsInternalBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);
        when(demandRepository.countFilledDemandsExternalBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);
        when(demandRepository.averageTimeToFillBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0.0);
        when(demandRepository.minTimeToFillBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0.0);
        when(demandRepository.maxTimeToFillBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0.0);
        when(demandRepository.countDemandsWithFilledPositionsBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);

        // Act
        DemandAnalyticsMetricsResponse response = analyticsService.getAnalytics(null, null);

        // Assert
        assertEquals(0.0, response.getFillRate().getFillRatePercent());
        assertEquals(0.0, response.getInternalVsExternalSplit().getInternalPercentage());
        assertEquals(0.0, response.getInternalVsExternalSplit().getExternalPercentage());
    }
}
