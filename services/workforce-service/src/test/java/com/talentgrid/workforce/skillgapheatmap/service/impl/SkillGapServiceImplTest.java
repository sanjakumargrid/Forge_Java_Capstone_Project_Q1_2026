package com.talentgrid.workforce.skillgapheatmap.service.impl;

import com.talentgrid.kafka.events.demand.DemandPayload;
import com.talentgrid.workforce.skillgapheatmap.dto.RefreshResponse;
import com.talentgrid.workforce.skillgapheatmap.provider.DemandProvider;
import com.talentgrid.workforce.skillgapheatmap.provider.WorkforceProvider;
import com.talentgrid.workforce.skillgapheatmap.provider.model.EngineerResponse;
import com.talentgrid.workforce.skillgapheatmap.repository.SkillGapAnalyticsQueryRepository;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapActiveDemandRegistry;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapRefreshCoordinator;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapSnapshotWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillGapServiceImpl")
class SkillGapServiceImplTest {

    @Mock
    private SkillGapAnalyticsQueryRepository queryRepository;

    @Mock
    private SkillGapRefreshCoordinator refreshCoordinator;

    @Mock
    private SkillGapSnapshotWriter snapshotWriter;

    @Mock
    private DemandProvider demandProvider;

    @Mock
    private WorkforceProvider workforceProvider;

    @Mock
    private SkillGapActiveDemandRegistry activeDemandRegistry;

    @InjectMocks
    private SkillGapServiceImpl skillGapService;

    @Test
    @DisplayName("refresh without HTTP auth uses registry and bench data")
    void refreshWithoutAuthUsesRegistryPath() {
        when(refreshCoordinator.isDebounced()).thenReturn(false);
        when(refreshCoordinator.tryAcquireLock()).thenReturn(true);
        when(activeDemandRegistry.aggregateDemandSkillCounts())
                .thenReturn(Map.of("Java", 2));
        when(workforceProvider.getBenchEngineers())
                .thenReturn(List.of(EngineerResponse.builder()
                        .employeeId(1L)
                        .skills(List.of("Java"))
                        .build()));
        when(snapshotWriter.fetchPreviousGapScores()).thenReturn(Map.of());

        RefreshResponse response = skillGapService.refresh();

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getProcessedSkills()).isEqualTo(1);
        verify(demandProvider, never()).getOpenDemands();
        verify(snapshotWriter).persistSnapshot(anyList(), any(), anyInt());
        verify(refreshCoordinator).markSuccess(any(RefreshResponse.class));
    }

    @Test
    @DisplayName("onActiveDemandEntered ignores non-active demand status")
    void onActiveDemandEnteredIgnoresNonActiveStatus() {
        DemandPayload payload = DemandPayload.builder()
                .demandId(10L)
                .status("APPROVED")
                .mandatorySkills(List.of("Java"))
                .build();

        skillGapService.onActiveDemandEntered(payload);

        verify(activeDemandRegistry, never()).upsertActiveDemand(any(), any());
    }

    @Test
    @DisplayName("onActiveDemandEntered upserts active demand and refreshes bench snapshot")
    void onActiveDemandEnteredUpsertsActiveDemand() {
        when(activeDemandRegistry.isActiveStatus("INTERNAL_SEARCH")).thenReturn(true);
        when(refreshCoordinator.isDebounced()).thenReturn(false);
        when(refreshCoordinator.tryAcquireLock()).thenReturn(true);
        when(activeDemandRegistry.aggregateDemandSkillCounts()).thenReturn(Map.of("Java", 1));
        when(workforceProvider.getBenchEngineers()).thenReturn(List.of());
        when(snapshotWriter.fetchPreviousGapScores()).thenReturn(Map.of());

        DemandPayload payload = DemandPayload.builder()
                .demandId(10L)
                .status("INTERNAL_SEARCH")
                .mandatorySkills(List.of("Java"))
                .build();

        skillGapService.onActiveDemandEntered(payload);

        verify(activeDemandRegistry).upsertActiveDemand(10L, List.of("Java"));
        verify(snapshotWriter).persistSnapshot(anyList(), any(), anyInt());
    }

    @Test
    @DisplayName("onActiveDemandRemoved deletes demand and refreshes bench snapshot")
    void onActiveDemandRemovedDeletesDemand() {
        when(refreshCoordinator.isDebounced()).thenReturn(false);
        when(refreshCoordinator.tryAcquireLock()).thenReturn(true);
        when(activeDemandRegistry.aggregateDemandSkillCounts()).thenReturn(Map.of());
        when(workforceProvider.getBenchEngineers()).thenReturn(List.of());
        when(snapshotWriter.fetchPreviousGapScores()).thenReturn(Map.of());

        skillGapService.onActiveDemandRemoved(42L);

        verify(activeDemandRegistry).removeDemand(42L);
        verify(snapshotWriter).persistSnapshot(anyList(), any(), anyInt());
    }
}
