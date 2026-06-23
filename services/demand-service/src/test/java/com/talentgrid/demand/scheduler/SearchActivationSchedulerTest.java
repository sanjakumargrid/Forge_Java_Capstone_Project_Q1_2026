package com.talentgrid.demand.scheduler;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.statemachine.TransitionValidator;
import com.talentgrid.demand.kafka.producer.DemandEventProducer;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.repository.DemandStatusHistoryRepository;
import com.talentgrid.demand.service.DemandLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchActivationSchedulerTest {

    @Mock
    private DemandRepository demandRepository;
    @Mock
    private DemandStatusHistoryRepository historyRepository;
    @Mock
    private TransitionValidator transitionValidator;
    @Mock
    private DemandMapper demandMapper;
    @Mock
    private DemandEventProducer eventProducer;
    @Mock
    private AuditLogClient auditLogClient;
    @Mock
    private UserAuthServiceClient userAuthServiceClient;

    @InjectMocks
    private DemandLifecycleService lifecycleService;

    private SearchActivationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SearchActivationScheduler(lifecycleService);
        lenient().when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().doNothing().when(transitionValidator).validate(any(Demand.class), any(DemandStatus.class), any());
    }

    @Test
    void activateApprovedDemands_transitionsToInternalSearch() {
        Demand demand = approvedDemand(1L, false);
        when(demandRepository.findByStatusAndIsDeletedFalse(DemandStatus.APPROVED))
                .thenReturn(List.of(demand));

        scheduler.activateApprovedDemands();

        assertEquals(DemandStatus.INTERNAL_SEARCH, demand.getStatus());
        assertNotNull(demand.getSearchStartAt());
        verify(eventProducer).publishApproved(demand);
        verify(eventProducer, never()).publishExternalOpened(any());
    }

    @Test
    void activateApprovedDemands_benchHiringOpensExternal() {
        Demand demand = approvedDemand(2L, true);
        when(demandRepository.findByStatusAndIsDeletedFalse(DemandStatus.APPROVED))
                .thenReturn(List.of(demand));

        scheduler.activateApprovedDemands();

        assertEquals(DemandStatus.OPEN_EXTERNAL, demand.getStatus());
        verify(eventProducer).publishApproved(demand);
        verify(eventProducer).publishExternalOpened(demand);
    }

    @Test
    void activateApprovedDemands_skipsWhenNoneFound() {
        when(demandRepository.findByStatusAndIsDeletedFalse(DemandStatus.APPROVED))
                .thenReturn(List.of());

        scheduler.activateApprovedDemands();

        verifyNoInteractions(eventProducer);
    }

    private static Demand approvedDemand(long id, boolean benchHiring) {
        Demand demand = new Demand();
        demand.setDemandId(id);
        demand.setStatus(DemandStatus.APPROVED);
        demand.setBenchHiring(benchHiring);
        demand.setIsDeleted(false);
        demand.setApprovedAt(OffsetDateTime.now().minusHours(1));
        demand.setApprovedBy(42L);
        return demand;
    }
}
