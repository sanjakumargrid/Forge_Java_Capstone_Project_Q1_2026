package com.talentgrid.demand.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.enums.WorkMode;
import com.talentgrid.demand.dto.request.DemandRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.exception.InvalidDemandStateException;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.repository.DemandStatusHistoryRepository;
import com.talentgrid.shared.auth.security.JwtPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandServiceUpdateFlowTest {

    @Mock private DemandRepository demandRepository;
    @Mock private DemandStatusHistoryRepository demandStatusHistoryRepository;
    @Spy private DemandMapper demandMapper = new DemandMapper();
    @Spy private DemandValidationService validationService = new DemandValidationService();
    @Mock private AuditLogClient auditLogClient;
    @Mock private UserAuthServiceClient userAuthServiceClient;
    @Mock private JobTitleLookupService jobTitleLookupService;
    @Mock private SkillLookupService skillLookupService;

    @InjectMocks
    private DemandService demandService;

    private Demand demand;

    @BeforeEach
    void setUp() {
        JwtPrincipal principal = JwtPrincipal.builder().userId(42L).email("hm@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));

        demand = new Demand();
        demand.setDemandId(7L);
        demand.setTitle("Senior Java Engineer");
        demand.setStatus(DemandStatus.DRAFT);
        demand.setLocation("Remote");
        demand.setPriority(DemandPriority.MEDIUM);
        demand.setWorkMode(WorkMode.REMOTE);

        lenient().when(demandRepository.findByDemandIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(demand));
        lenient().when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(demandStatusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validateUpdate_missingReasonForEdit_rejected() {
        DemandRequest request = new DemandRequest();
        request.setLocation("Bangalore");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateUpdate(request));
        assertTrue(ex.getMessage().contains("reasonForEdit is required"));
    }

    @Test
    void validateUpdate_blankReasonForEdit_rejected() {
        DemandRequest request = new DemandRequest();
        request.setReasonForEdit("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateUpdate(request));
        assertTrue(ex.getMessage().contains("reasonForEdit is required"));
    }

    @Test
    void validateUpdate_reasonForEditTooLong_rejected() {
        DemandRequest request = new DemandRequest();
        request.setReasonForEdit("x".repeat(1001));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateUpdate(request));
        assertTrue(ex.getMessage().contains("reasonForEdit must not exceed 1000 characters"));
    }

    @ParameterizedTest
    @EnumSource(value = DemandStatus.class, names = {"FILLED", "CLOSED"})
    void updateDemand_terminalStatus_rejected(DemandStatus status) {
        demand.setStatus(status);
        DemandRequest request = updateRequest("Budget revision");
        request.setBudget(java.math.BigDecimal.valueOf(200000));

        InvalidDemandStateException ex = assertThrows(InvalidDemandStateException.class,
                () -> demandService.updateDemand(7L, request));
        assertTrue(ex.getMessage().contains("editing is not allowed"));
        verify(demandRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = DemandStatus.class, names = {
            "DRAFT", "PENDING_APPROVAL", "APPROVED", "INTERNAL_SEARCH", "OPEN_EXTERNAL", "ON_HOLD"
    })
    void updateDemand_editableStatuses_allowed(DemandStatus status) {
        demand.setStatus(status);
        DemandRequest request = updateRequest("Location change");
        request.setLocation("Hyderabad");

        DemandResponse response = demandService.updateDemand(7L, request);

        assertNotNull(response);
        verify(demandRepository).save(any(Demand.class));
        verify(demandStatusHistoryRepository).save(any(DemandStatusHistory.class));
    }

    @Test
    void updateDemand_pendingApproval_canChangeLockedFields() {
        demand.setStatus(DemandStatus.PENDING_APPROVAL);
        DemandRequest request = updateRequest("Priority bump before approval");
        request.setPriority(DemandPriority.HIGH);

        demandService.updateDemand(7L, request);

        assertEquals(DemandPriority.HIGH, demand.getPriority());
    }

    @ParameterizedTest
    @EnumSource(value = DemandStatus.class, names = {"APPROVED", "INTERNAL_SEARCH", "OPEN_EXTERNAL", "ON_HOLD"})
    void updateDemand_postApproval_lockedFieldRejected(DemandStatus status) {
        demand.setStatus(status);
        DemandRequest request = updateRequest("Attempt to change priority");
        request.setPriority(DemandPriority.CRITICAL);

        InvalidDemandStateException ex = assertThrows(InvalidDemandStateException.class,
                () -> demandService.updateDemand(7L, request));
        assertTrue(ex.getMessage().contains("locked field"));
        assertTrue(ex.getMessage().contains("Priority"));
        verify(demandRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = DemandStatus.class, names = {"APPROVED", "INTERNAL_SEARCH", "OPEN_EXTERNAL", "ON_HOLD"})
    void updateDemand_postApproval_allowedFieldsPersisted(DemandStatus status) {
        demand.setStatus(status);
        DemandRequest request = updateRequest("Adjust search timeline and budget");
        request.setLocation("Pune");
        request.setWorkMode(WorkMode.HYBRID);
        request.setBudget(java.math.BigDecimal.valueOf(175000));
        request.setReqUtilPerc(90);
        request.setExperience(8L);
        request.setDescription("Updated job description after approval");

        demandService.updateDemand(7L, request);

        assertEquals("Pune", demand.getLocation());
        assertEquals(WorkMode.HYBRID, demand.getWorkMode());
        assertEquals(java.math.BigDecimal.valueOf(175000), demand.getBudget());
        assertEquals(90, demand.getReqUtilPerc());
        assertEquals(8L, demand.getExperience());
        assertEquals("Updated job description after approval", demand.getDescription());
    }

    @Test
    void updateDemand_persistsEditHistoryAndAuditPayload() {
        demand.setStatus(DemandStatus.APPROVED);
        DemandRequest request = updateRequest("Client requested location change");
        request.setLocation("Chennai");

        demandService.updateDemand(7L, request);

        ArgumentCaptor<DemandStatusHistory> historyCaptor = ArgumentCaptor.forClass(DemandStatusHistory.class);
        verify(demandStatusHistoryRepository).save(historyCaptor.capture());
        DemandStatusHistory history = historyCaptor.getValue();
        assertEquals(DemandStatus.APPROVED, history.getFromStatus());
        assertEquals(DemandStatus.APPROVED, history.getToStatus());
        assertEquals("Client requested location change", history.getComments());
        assertEquals(42L, history.getChangedBy());

        ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
        verify(auditLogClient).logAction(auditCaptor.capture());
        AuditLogPayload audit = auditCaptor.getValue();
        assertEquals(AuditAction.UPDATE, audit.getAction());
        assertEquals(7L, audit.getEntityId());
        assertEquals("Client requested location change", audit.getReasonForEdit());
        assertNotNull(audit.getBeforeState());
        assertNotNull(audit.getAfterState());
        assertEquals("Remote", audit.getBeforeState().get("location"));
        assertEquals("Chennai", audit.getAfterState().get("location"));
    }

    @Test
    void updateDemand_returnsSameDemandId_notNewRecord() {
        DemandRequest request = updateRequest("Minor edit");
        request.setLocation("Mumbai");

        ArgumentCaptor<Demand> savedCaptor = ArgumentCaptor.forClass(Demand.class);
        when(demandMapper.toResponse(any())).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            DemandResponse response = new DemandResponse();
            response.setDemandId(saved.getDemandId());
            response.setLocation(saved.getLocation());
            return response;
        });

        DemandResponse response = demandService.updateDemand(7L, request);

        verify(demandRepository).save(savedCaptor.capture());
        assertEquals(7L, savedCaptor.getValue().getDemandId());
        assertEquals(7L, response.getDemandId());
        assertEquals("Mumbai", response.getLocation());
    }

    private static DemandRequest updateRequest(String reason) {
        DemandRequest request = new DemandRequest();
        request.setReasonForEdit(reason);
        return request;
    }
}
