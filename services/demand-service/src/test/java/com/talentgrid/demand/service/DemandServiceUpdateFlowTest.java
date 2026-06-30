package com.talentgrid.demand.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.client.dto.AccountDto;
import com.talentgrid.demand.client.dto.ProjectDto;
import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.enums.WorkMode;
import com.talentgrid.demand.dto.request.DemandRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.repository.DemandSkillRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandServiceUpdateFlowTest {

    @Mock private DemandRepository demandRepository;
    @Mock private DemandStatusHistoryRepository demandStatusHistoryRepository;
    @Mock private DemandSkillRepository demandSkillRepository;
    @Spy private DemandMapper demandMapper = new DemandMapper();
    @Mock private SeniorityLevelLookupService seniorityLevelLookupService;
    @Mock private AuditLogClient auditLogClient;
    @Mock private UserAuthServiceClient userAuthServiceClient;
    @Mock private JobTitleLookupService jobTitleLookupService;
    @Mock private SkillLookupService skillLookupService;

    @InjectMocks
    private DemandService demandService;

    private DemandValidationService validationService;
    private Demand demand;

    @BeforeEach
    void setUp() {
        validationService = spy(new DemandValidationService(seniorityLevelLookupService));
        ReflectionTestUtils.setField(demandService, "validationService", validationService);

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
                () -> validationService.validateUpdate(request, "PENDING_APPROVAL"));
        assertTrue(ex.getMessage().contains("reasonForEdit is required"));
    }

    @Test
    void validateUpdate_draftWithoutReasonForEdit_allowed() {
        DemandRequest request = new DemandRequest();
        request.setLocation("Bangalore");

        assertDoesNotThrow(() -> validationService.validateUpdate(request, "DRAFT"));
    }

    @Test
    void updateDemand_draftWithoutReason_skipsEditHistory() {
        demand.setStatus(DemandStatus.DRAFT);
        DemandRequest request = new DemandRequest();
        request.setLocation("Bangalore");

        demandService.updateDemand(7L, request);

        verify(demandRepository).save(any(Demand.class));
        verify(demandStatusHistoryRepository, never()).save(any(DemandStatusHistory.class));
    }

    @Test
    void validateUpdate_approvedWithoutReasonForEdit_rejected() {
        DemandRequest request = new DemandRequest();
        request.setLocation("Bangalore");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateUpdate(request, "APPROVED"));
        assertTrue(ex.getMessage().contains("reasonForEdit is required"));
    }

    @Test
    void validateUpdate_blankReasonForEdit_rejected() {
        DemandRequest request = new DemandRequest();
        request.setReasonForEdit("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateUpdate(request, "PENDING_APPROVAL"));
        assertTrue(ex.getMessage().contains("reasonForEdit is required"));
    }

    @Test
    void validateUpdate_reasonForEditTooLong_rejected() {
        DemandRequest request = new DemandRequest();
        request.setReasonForEdit("x".repeat(1001));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateUpdate(request, "PENDING_APPROVAL"));
        assertTrue(ex.getMessage().contains("reasonForEdit must not exceed 1000 characters"));
    }

    @ParameterizedTest
    @EnumSource(value = DemandStatus.class, names = {"FILLED", "CLOSED"})
    void updateDemand_terminalStatus_rejected(DemandStatus status) {
        demand.setStatus(status);
        DemandRequest request = updateRequest("Budget revision");
        request.setBudget(java.math.BigDecimal.valueOf(200000));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> demandService.updateDemand(7L, request));
        assertTrue(ex.getMessage().contains("Cannot edit a demand in " + status + " status"));
        verify(demandRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = DemandStatus.class, names = {
            "DRAFT", "PENDING_APPROVAL", "APPROVED", "INTERNAL_SEARCH", "OPEN_EXTERNAL", "ON_HOLD"
    })
    void updateDemand_editableStatuses_allowed(DemandStatus status) {
        demand.setStatus(status);
        DemandRequest request = status == DemandStatus.DRAFT
                ? new DemandRequest()
                : updateRequest("Editable status update");
        request.setLocation("Hyderabad");

        DemandResponse response = demandService.updateDemand(7L, request);

        assertNotNull(response);
        verify(demandRepository).save(any(Demand.class));
        if (status == DemandStatus.DRAFT) {
            verify(demandStatusHistoryRepository, never()).save(any(DemandStatusHistory.class));
        } else {
            verify(demandStatusHistoryRepository).save(any(DemandStatusHistory.class));
        }
    }

    @Test
    void updateDemand_pendingApproval_canChangeLockedFields() {
        demand.setStatus(DemandStatus.PENDING_APPROVAL);
        DemandRequest request = updateRequest("Priority bump before approval");
        request.setPriority(DemandPriority.HIGH);

        demandService.updateDemand(7L, request);

        assertEquals(DemandPriority.HIGH, demand.getPriority());
    }

    @Test
    void updateDemand_approved_canChangePriority() {
        demand.setStatus(DemandStatus.APPROVED);
        DemandRequest request = updateRequest("Priority bump after approval");
        request.setPriority(DemandPriority.HIGH);

        demandService.updateDemand(7L, request);

        assertEquals(DemandPriority.HIGH, demand.getPriority());
    }

    @ParameterizedTest
    @EnumSource(value = DemandStatus.class, names = {"INTERNAL_SEARCH", "OPEN_EXTERNAL", "ON_HOLD"})
    void updateDemand_partialEditLock_lockedFieldRejected(DemandStatus status) {
        demand.setStatus(status);
        DemandRequest request = updateRequest("Attempt to change priority");
        request.setPriority(DemandPriority.CRITICAL);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> demandService.updateDemand(7L, request));
        assertTrue(ex.getMessage().contains("locked field"));
        assertTrue(ex.getMessage().contains("priority"));
        verify(demandRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = DemandStatus.class, names = {"INTERNAL_SEARCH", "OPEN_EXTERNAL", "ON_HOLD"})
    void updateDemand_partialEditLock_otherFieldsPersisted(DemandStatus status) {
        demand.setStatus(status);
        DemandRequest request = updateRequest("Adjust search timeline and budget");
        request.setLocation("Pune");
        request.setWorkMode(WorkMode.HYBRID);
        request.setBudget(java.math.BigDecimal.valueOf(175000));
        request.setReqUtilPerc(90);
        request.setExperience(8L);
        request.setDescription("Updated job description during search");

        demandService.updateDemand(7L, request);

        assertEquals("Pune", demand.getLocation());
        assertEquals(WorkMode.HYBRID, demand.getWorkMode());
        assertEquals(java.math.BigDecimal.valueOf(175000), demand.getBudget());
        assertEquals(90, demand.getReqUtilPerc());
        assertEquals(8L, demand.getExperience());
        assertEquals("Updated job description during search", demand.getDescription());
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

    @Test
    void updateDemand_newProjectId_resolvesProjectAndAccountNames() {
        demand.setProjectId(10L);
        demand.setProjectName("Old Project");
        demand.setAccountId(100L);
        demand.setAccountName("Old Account");

        DemandRequest request = updateRequest("Reassign to different project");
        request.setProjectId(20L);

        when(userAuthServiceClient.getProjectById(20L)).thenReturn(
                ProjectDto.builder().id(20L).name("New Project").accountId(200L).build());
        when(userAuthServiceClient.getAccountById(200L)).thenReturn(
                AccountDto.builder().id(200L).name("New Account").build());

        demandService.updateDemand(7L, request);

        assertEquals(20L, demand.getProjectId());
        assertEquals("New Project", demand.getProjectName());
        assertEquals(200L, demand.getAccountId());
        assertEquals("New Account", demand.getAccountName());
        verify(userAuthServiceClient).getProjectById(20L);
        verify(userAuthServiceClient).getAccountById(200L);
    }

    private static DemandRequest updateRequest(String reason) {
        DemandRequest request = new DemandRequest();
        request.setReasonForEdit(reason);
        return request;
    }
}
