package com.talentgrid.workforce.skillgapheatmap.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.demand.DemandPayload;
import com.talentgrid.workforce.skillgapheatmap.service.SkillGapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillGapDemandEventConsumer")
class SkillGapDemandEventConsumerTest {

    @Mock
    private SkillGapService skillGapService;

    private SkillGapDemandEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SkillGapDemandEventConsumer(skillGapService, new ObjectMapper());
    }

    @Test
    @DisplayName("DEMAND_APPROVED with APPROVED status registers demand")
    void demandApprovedWithApprovedStatusRegistersDemand() {
        BaseEvent<Map<String, Object>> event = new BaseEvent<>();
        event.setEventType("DEMAND_APPROVED");
        event.setEventId("evt-approved");
        event.setPayload(Map.of(
                "demandId", 6L,
                "status", "APPROVED",
                "mandatorySkills", List.of("Python")
        ));

        consumer.consume(event);

        verify(skillGapService).onActiveDemandEntered(any(DemandPayload.class));
    }

    @Test
    @DisplayName("DEMAND_APPROVED with active status registers demand")
    void demandApprovedRegistersActiveDemand() {
        BaseEvent<Map<String, Object>> event = new BaseEvent<>();
        event.setEventType("DEMAND_APPROVED");
        event.setEventId("evt-1");
        event.setPayload(Map.of(
                "demandId", 5L,
                "status", "INTERNAL_SEARCH",
                "mandatorySkills", List.of("Java")
        ));

        consumer.consume(event);

        verify(skillGapService).onActiveDemandEntered(any(DemandPayload.class));
    }

    @Test
    @DisplayName("DEMAND_CLOSED removes demand from registry")
    void demandClosedRemovesDemand() {
        BaseEvent<Map<String, Object>> event = new BaseEvent<>();
        event.setEventType("DEMAND_CLOSED");
        event.setEventId("evt-2");
        event.setPayload(Map.of("demandId", 9L, "status", "CLOSED"));

        consumer.consume(event);

        verify(skillGapService).onActiveDemandRemoved(eq(9L));
        verify(skillGapService, never()).onActiveDemandEntered(any(DemandPayload.class));
    }
}
