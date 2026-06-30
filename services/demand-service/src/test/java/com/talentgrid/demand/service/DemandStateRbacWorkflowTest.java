package com.talentgrid.demand.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.ClosureReason;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.statemachine.DemandStateMachine;
import com.talentgrid.demand.domain.statemachine.TransitionValidator;
import com.talentgrid.demand.dto.request.ApprovalRequest;
import com.talentgrid.demand.dto.request.StatusTransitionRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.exception.IllegalDemandTransitionException;
import com.talentgrid.demand.kafka.producer.DemandEventProducer;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.repository.DemandStatusHistoryRepository;
import com.talentgrid.shared.auth.security.JwtPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DemandStateRbacWorkflowTest {

    @Mock
    private DemandRepository demandRepository;
    @Mock
    private DemandStatusHistoryRepository historyRepository;
    @Mock
    private DemandMapper demandMapper;
    @Mock
    private DemandEventProducer eventProducer;
    @Mock
    private AuditLogClient auditLogClient;
    @Mock
    private UserAuthServiceClient userAuthServiceClient;

    private TransitionValidator transitionValidator;
    private DemandLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        DemandStateMachine stateMachine = new DemandStateMachine();
        transitionValidator = new TransitionValidator(stateMachine);
        lifecycleService = new DemandLifecycleService(
                demandRepository,
                historyRepository,
                transitionValidator,
                demandMapper,
                eventProducer,
                auditLogClient,
                userAuthServiceClient
        );
        lenient().when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void login(long userId, String role) {
        JwtPrincipal principal = JwtPrincipal.builder()
                .userId(userId)
                .email(role.toLowerCase() + "@example.com")
                .roles(List.of(role))
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Demand createDemand(long demandId, DemandStatus status, long creatorId) {
        Demand d = new Demand();
        d.setDemandId(demandId);
        d.setStatus(status);
        d.setCreatedBy(creatorId);
        d.setIsDeleted(false);
        return d;
    }

    @Test
    void testDraftToPendingApproval_allowedForCreatorHm_deniedForRecruiter() {
        Demand demand = createDemand(1L, DemandStatus.DRAFT, 10L); // created by user 10
        when(demandRepository.findByDemandIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(demand));

        // 1. Recruiter tries to submit -> fails
        login(30L, "RECRUITER");
        assertThrows(AccessDeniedException.class, () -> {
            lifecycleService.transitionStatus(1L, StatusTransitionRequest.builder()
                    .targetStatus(DemandStatus.PENDING_APPROVAL)
                    .build());
        });

        // 2. Hiring Manager (who is NOT the creator) tries to submit -> fails
        login(20L, "HIRING_MANAGER");
        assertThrows(AccessDeniedException.class, () -> {
            lifecycleService.transitionStatus(1L, StatusTransitionRequest.builder()
                    .targetStatus(DemandStatus.PENDING_APPROVAL)
                    .build());
        });

        // 3. Creator Hiring Manager submits -> succeeds
        login(10L, "HIRING_MANAGER");
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        DemandResponse response = lifecycleService.transitionStatus(1L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.PENDING_APPROVAL)
                .build());

        assertEquals("PENDING_APPROVAL", response.getStatus());
    }

    @Test
    void testInternalSearchToHold_allowedForRm_deniedForHm() {
        Demand demand = createDemand(2L, DemandStatus.INTERNAL_SEARCH, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(demand));

        // 1. HM tries to place on hold -> fails
        login(10L, "HIRING_MANAGER");
        assertThrows(AccessDeniedException.class, () -> {
            lifecycleService.transitionStatus(2L, StatusTransitionRequest.builder()
                    .targetStatus(DemandStatus.ON_HOLD)
                    .closureReason(ClosureReason.ON_HOLD)
                    .build());
        });

        // 2. RM places on hold -> succeeds
        login(40L, "RESOURCE_MANAGER");
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        DemandResponse response = lifecycleService.transitionStatus(2L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.ON_HOLD)
                .closureReason(ClosureReason.ON_HOLD)
                .build());

        assertEquals("ON_HOLD", response.getStatus());
        assertEquals(DemandStatus.INTERNAL_SEARCH, demand.getPreviousStatus());
    }

    @Test
    void testOpenExternalToHold_allowedForRecruiterAndRm() {
        Demand demand = createDemand(3L, DemandStatus.OPEN_EXTERNAL, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(3L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        // 1. RM places on hold -> succeeds
        login(40L, "RESOURCE_MANAGER");
        DemandResponse rmResponse = lifecycleService.transitionStatus(3L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.ON_HOLD)
                .closureReason(ClosureReason.ON_HOLD)
                .build());
        assertEquals("ON_HOLD", rmResponse.getStatus());
        assertEquals(DemandStatus.OPEN_EXTERNAL, demand.getPreviousStatus());

        demand.setStatus(DemandStatus.OPEN_EXTERNAL);
        demand.setPreviousStatus(null);

        // 2. Recruiter places on hold -> succeeds
        login(50L, "RECRUITER");
        DemandResponse recruiterResponse = lifecycleService.transitionStatus(3L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.ON_HOLD)
                .closureReason(ClosureReason.ON_HOLD)
                .build());

        assertEquals("ON_HOLD", recruiterResponse.getStatus());
        assertEquals(DemandStatus.OPEN_EXTERNAL, demand.getPreviousStatus());
    }

    @Test
    void testOnHoldResume_allowedOnlyForDesignatedRolesAndPreviousState() {
        // Scenario 1: Demand was in INTERNAL_SEARCH, only RM can resume
        Demand demand1 = createDemand(4L, DemandStatus.ON_HOLD, 10L);
        demand1.setPreviousStatus(DemandStatus.INTERNAL_SEARCH);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(4L)).thenReturn(Optional.of(demand1));

        // Recruiter tries to resume -> fails
        login(50L, "RECRUITER");
        assertThrows(AccessDeniedException.class, () -> {
            lifecycleService.transitionStatus(4L, StatusTransitionRequest.builder()
                    .targetStatus(DemandStatus.INTERNAL_SEARCH)
                    .build());
        });

        // RM resumes -> succeeds
        login(40L, "RESOURCE_MANAGER");
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        DemandResponse response1 = lifecycleService.transitionStatus(4L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.INTERNAL_SEARCH)
                .build());
        assertEquals("INTERNAL_SEARCH", response1.getStatus());

        // Scenario 2: Demand was in OPEN_EXTERNAL, RM or recruiter may resume
        Demand demand2 = createDemand(5L, DemandStatus.ON_HOLD, 10L);
        demand2.setPreviousStatus(DemandStatus.OPEN_EXTERNAL);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(demand2));

        // RM resumes -> succeeds
        login(40L, "RESOURCE_MANAGER");
        DemandResponse response2 = lifecycleService.transitionStatus(5L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.OPEN_EXTERNAL)
                .build());
        assertEquals("OPEN_EXTERNAL", response2.getStatus());

        demand2.setStatus(DemandStatus.ON_HOLD);
        demand2.setPreviousStatus(DemandStatus.OPEN_EXTERNAL);

        // Recruiter resumes -> succeeds
        login(50L, "RECRUITER");
        DemandResponse response3 = lifecycleService.transitionStatus(5L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.OPEN_EXTERNAL)
                .build());
        assertEquals("OPEN_EXTERNAL", response3.getStatus());
    }

    @Test
    void testOnHoldToClosed_allowedOnlyForRmWithCorrectReason() {
        Demand demand = createDemand(6L, DemandStatus.ON_HOLD, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(6L)).thenReturn(Optional.of(demand));

        // 1. Recruiter tries to close -> fails
        login(50L, "RECRUITER");
        assertThrows(AccessDeniedException.class, () -> {
            lifecycleService.transitionStatus(6L, StatusTransitionRequest.builder()
                    .targetStatus(DemandStatus.CLOSED)
                    .closureReason(ClosureReason.RM_CLOSED_ON_HOLD)
                    .build());
        });

        // 2. RM tries to close with incorrect reason -> fails validation (not RBAC)
        login(40L, "RESOURCE_MANAGER");
        assertThrows(IllegalDemandTransitionException.class, () -> {
            lifecycleService.transitionStatus(6L, StatusTransitionRequest.builder()
                    .targetStatus(DemandStatus.CLOSED)
                    .closureReason(ClosureReason.SLA_APPROVAL_BREACH)
                    .build());
        });

        // 3. RM closes with RM_CLOSED_ON_HOLD -> succeeds
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        DemandResponse response = lifecycleService.transitionStatus(6L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.CLOSED)
                .closureReason(ClosureReason.RM_CLOSED_ON_HOLD)
                .build());
        assertEquals("CLOSED", response.getStatus());
    }

    @Test
    void testInternalSearchToFilled_allowedForHmAndRm() {
        Demand demand = createDemand(7L, DemandStatus.INTERNAL_SEARCH, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        // 1. RM accepts internal fill -> succeeds and auto-closes
        login(40L, "RESOURCE_MANAGER");
        DemandResponse rmResponse = lifecycleService.transitionStatus(7L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.FILLED)
                .closureReason(ClosureReason.FILLED)
                .build());
        assertEquals("CLOSED", rmResponse.getStatus());

        demand.setStatus(DemandStatus.INTERNAL_SEARCH);

        // 2. HM accepts internal fill -> succeeds and auto-closes
        login(10L, "HIRING_MANAGER");
        DemandResponse hmResponse = lifecycleService.transitionStatus(7L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.FILLED)
                .closureReason(ClosureReason.FILLED)
                .build());
        assertEquals("CLOSED", hmResponse.getStatus());
    }

    @Test
    void testOpenExternalToFilled_allowedForTaManager_deniedForHm() {
        Demand demand = createDemand(8L, DemandStatus.OPEN_EXTERNAL, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(8L)).thenReturn(Optional.of(demand));

        // 1. HM tries to fill -> fails
        login(10L, "HIRING_MANAGER");
        assertThrows(AccessDeniedException.class, () -> {
            lifecycleService.transitionStatus(8L, StatusTransitionRequest.builder()
                    .targetStatus(DemandStatus.FILLED)
                    .closureReason(ClosureReason.FILLED)
                    .build());
        });

        // 2. TA Manager fills -> succeeds and auto-closes
        login(60L, "TA_MANAGER");
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        DemandResponse response = lifecycleService.transitionStatus(8L, StatusTransitionRequest.builder()
                    .targetStatus(DemandStatus.FILLED)
                    .closureReason(ClosureReason.FILLED)
                .build());
        assertEquals("CLOSED", response.getStatus());
    }

    @Test
    void testInternalSearchToOpenExternal_enforcesFiveDayGateUnlessBypassed() {
        // Scenario 1: Under 5 days, no bypass -> fails
        Demand demand = createDemand(9L, DemandStatus.INTERNAL_SEARCH, 10L);
        demand.setSearchStartAt(OffsetDateTime.now().minusDays(2)); // Only 2 days elapsed
        when(demandRepository.findByDemandIdAndIsDeletedFalse(9L)).thenReturn(Optional.of(demand));

        login(40L, "RESOURCE_MANAGER");
        assertThrows(IllegalDemandTransitionException.class, () -> {
            lifecycleService.transitionStatus(9L, StatusTransitionRequest.builder()
                    .targetStatus(DemandStatus.OPEN_EXTERNAL)
                    .build());
        });

        // Scenario 2: Under 5 days, but bypassed with NO_INTERNAL_MATCH -> succeeds
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        DemandResponse response = lifecycleService.transitionStatus(9L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.OPEN_EXTERNAL)
                .closureReason(ClosureReason.NO_INTERNAL_MATCH)
                .build());
        assertEquals("OPEN_EXTERNAL", response.getStatus());
    }

    @Test
    void adminMayPerformAnyLegalTransition_withoutRoleSpecificOwnership() {
        Demand demand = createDemand(10L, DemandStatus.INTERNAL_SEARCH, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        login(99L, "ADMIN");
        DemandResponse response = lifecycleService.transitionStatus(10L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.ON_HOLD)
                .closureReason(ClosureReason.ON_HOLD)
                .build());

        assertEquals("ON_HOLD", response.getStatus());
    }

    @Test
    void adminMayApprovePendingDemand_withoutProjectManagerCheck() {
        Demand demand = createDemand(11L, DemandStatus.PENDING_APPROVAL, 10L);
        demand.setProjectId(100L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(11L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        login(99L, "ADMIN");
        DemandResponse response = lifecycleService.approve(11L, ApprovalRequest.builder()
                .decision(DemandStatus.APPROVED)
                .build());

        assertEquals("APPROVED", response.getStatus());
        verify(userAuthServiceClient, never()).getProjectById(any());
    }

    @Test
    void adminMaySubmitDraftAndAutoApprove() {
        Demand demand = createDemand(12L, DemandStatus.DRAFT, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(12L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        login(99L, "ADMIN");
        DemandResponse response = lifecycleService.submitDemand(12L, "Admin override");

        assertEquals("APPROVED", response.getStatus());
    }

    @Test
    void hmMayRejectNominationAndOpenExternal() {
        Demand demand = createDemand(14L, DemandStatus.INTERNAL_SEARCH, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(14L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        login(10L, "HIRING_MANAGER");
        DemandResponse response = lifecycleService.transitionStatus(14L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.OPEN_EXTERNAL)
                .closureReason(ClosureReason.HM_REJECTED_NOMINATION)
                .build());

        assertEquals("OPEN_EXTERNAL", response.getStatus());
    }

    @Test
    void resourceManagerMayRejectNominationAndOpenExternal() {
        Demand demand = createDemand(15L, DemandStatus.INTERNAL_SEARCH, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(15L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        login(40L, "RESOURCE_MANAGER");
        DemandResponse response = lifecycleService.transitionStatus(15L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.OPEN_EXTERNAL)
                .closureReason(ClosureReason.HM_REJECTED_NOMINATION)
                .build());

        assertEquals("OPEN_EXTERNAL", response.getStatus());
    }

    @Test
    void resourceManagerMayPerformAnyLegalTransition_withoutRoleSpecificOwnership() {
        Demand demand = createDemand(17L, DemandStatus.OPEN_EXTERNAL, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(17L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        login(40L, "RESOURCE_MANAGER");
        DemandResponse response = lifecycleService.transitionStatus(17L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.FILLED)
                .closureReason(ClosureReason.FILLED)
                .build());

        assertEquals("CLOSED", response.getStatus());
    }

    @Test
    void resourceManagerMaySubmitDraftAndAutoApprove() {
        Demand demand = createDemand(18L, DemandStatus.DRAFT, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(18L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        login(40L, "RESOURCE_MANAGER");
        DemandResponse response = lifecycleService.submitDemand(18L, "RM override");

        assertEquals("APPROVED", response.getStatus());
    }

    @Test
    void resourceManagerMayApprovePendingDemand_withoutProjectManagerCheck() {
        Demand demand = createDemand(19L, DemandStatus.PENDING_APPROVAL, 10L);
        demand.setProjectId(100L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(19L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        login(40L, "RESOURCE_MANAGER");
        DemandResponse response = lifecycleService.approve(19L, ApprovalRequest.builder()
                .decision(DemandStatus.APPROVED)
                .build());

        assertEquals("APPROVED", response.getStatus());
        verify(userAuthServiceClient, never()).getProjectById(any());
    }

    @Test
    void portfolioManagerPatchStatus_deniedForNonAdminTransition() {
        Demand demand = createDemand(16L, DemandStatus.INTERNAL_SEARCH, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(16L)).thenReturn(Optional.of(demand));

        login(20L, "PORTFOLIO_MANAGER");
        assertThrows(AccessDeniedException.class, () -> {
            lifecycleService.transitionStatus(16L, StatusTransitionRequest.builder()
                    .targetStatus(DemandStatus.ON_HOLD)
                    .closureReason(ClosureReason.ON_HOLD)
                    .build());
        });
    }

    // ─── PENDING_APPROVAL → CLOSED tests ─────────────────────────────────────

    @Test
    void pendingApprovalToClosed_ownerHmWithHmClosed_succeeds() {
        // HM who created the demand closes it with HM_CLOSED → 200, status=CLOSED
        Demand demand = createDemand(20L, DemandStatus.PENDING_APPROVAL, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        login(10L, "HIRING_MANAGER"); // userId 10 is the creator
        DemandResponse response = lifecycleService.transitionStatus(20L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.CLOSED)
                .closureReason(ClosureReason.HM_CLOSED)
                .build());

        assertEquals("CLOSED", response.getStatus());
    }

    @Test
    void pendingApprovalToClosed_nonOwnerHm_throwsAccessDenied() {
        // HM who did NOT create the demand → 403
        Demand demand = createDemand(21L, DemandStatus.PENDING_APPROVAL, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(21L)).thenReturn(Optional.of(demand));

        login(99L, "HIRING_MANAGER"); // userId 99 ≠ createdBy 10
        assertThrows(AccessDeniedException.class, () ->
                lifecycleService.transitionStatus(21L, StatusTransitionRequest.builder()
                        .targetStatus(DemandStatus.CLOSED)
                        .closureReason(ClosureReason.HM_CLOSED)
                        .build()));
    }

    @Test
    void pendingApprovalToClosed_ownerHmWithWrongReason_throwsAccessDenied() {
        // Owner HM uses OTHER instead of HM_CLOSED → RBAC blocks it before TransitionValidator
        Demand demand = createDemand(22L, DemandStatus.PENDING_APPROVAL, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(22L)).thenReturn(Optional.of(demand));

        login(10L, "HIRING_MANAGER");
        assertThrows(AccessDeniedException.class, () ->
                lifecycleService.transitionStatus(22L, StatusTransitionRequest.builder()
                        .targetStatus(DemandStatus.CLOSED)
                        .closureReason(ClosureReason.OTHER)
                        .build()));
    }

    @Test
    void pendingApprovalToClosed_pmWithPmRejected_succeeds() {
        // Regression: PM closes PENDING_APPROVAL with PM_REJECTED → still works
        Demand demand = createDemand(23L, DemandStatus.PENDING_APPROVAL, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(23L)).thenReturn(Optional.of(demand));
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        login(20L, "PORTFOLIO_MANAGER");
        DemandResponse response = lifecycleService.transitionStatus(23L, StatusTransitionRequest.builder()
                .targetStatus(DemandStatus.CLOSED)
                .closureReason(ClosureReason.PM_REJECTED)
                .build());

        assertEquals("CLOSED", response.getStatus());
    }

    @Test
    void pendingApprovalToClosed_recruiter_throwsAccessDenied() {
        // Recruiter has no path → falls through to the final throw → 403
        Demand demand = createDemand(24L, DemandStatus.PENDING_APPROVAL, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(24L)).thenReturn(Optional.of(demand));

        login(50L, "RECRUITER");
        assertThrows(AccessDeniedException.class, () ->
                lifecycleService.transitionStatus(24L, StatusTransitionRequest.builder()
                        .targetStatus(DemandStatus.CLOSED)
                        .closureReason(ClosureReason.PM_REJECTED)
                        .build()));
    }

    @Test
    void adminStillBlockedByIllegalStateMachineTransition() {
        Demand demand = createDemand(13L, DemandStatus.CLOSED, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(13L)).thenReturn(Optional.of(demand));

        login(99L, "ADMIN");
        assertThrows(IllegalDemandTransitionException.class, () -> {
            lifecycleService.transitionStatus(13L, StatusTransitionRequest.builder()
                    .targetStatus(DemandStatus.INTERNAL_SEARCH)
                    .build());
        });
    }
}
