package com.talentgrid.demand.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditLogPayload;
import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.client.dto.ProjectDto;
import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.statemachine.TransitionValidator;
import com.talentgrid.demand.dto.request.ApprovalRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.kafka.producer.DemandEventProducer;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.repository.DemandStatusHistoryRepository;
import com.talentgrid.shared.auth.security.JwtPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies PM demand approval: project ownership gate + reuse of approval workflow.
 */
@ExtendWith(MockitoExtension.class)
class DemandLifecycleServicePmApprovalTest {

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

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void login(long userId) {
        JwtPrincipal principal = JwtPrincipal.builder()
                .userId(userId)
                .email("pm@example.com")
                .roles(List.of("PROJECT_MANAGER"))
                .scopes(List.of("DEMAND_PM_APPROVE"))
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("DEMAND_PM_APPROVE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Demand pendingDemand(long demandId, long projectId) {
        Demand d = new Demand();
        d.setDemandId(demandId);
        d.setStatus(DemandStatus.PENDING_APPROVAL);
        d.setProjectId(projectId);
        d.setIsDeleted(false);
        return d;
    }

    @BeforeEach
    void stubHistory() {
        lenient().when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void approveAsProjectManager_whenUserIsProjectManager_transitionsAndAuditsWithPmEndpoint() {
        login(42L);
        Demand demand = pendingDemand(7L, 100L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(demand));
        when(userAuthServiceClient.getProjectById(100L)).thenReturn(
                ProjectDto.builder().id(100L).projectManagerId(42L).build());
        doNothing().when(transitionValidator).validate(any(Demand.class), any(DemandStatus.class), any());
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder()
                    .demandId(saved.getDemandId())
                    .status(saved.getStatus().name())
                    .build();
        });

        ApprovalRequest body = ApprovalRequest.builder()
                .decision(DemandStatus.APPROVED)
                .comments("LGTM")
                .build();

        DemandResponse response = lifecycleService.approveAsProjectManager(7L, body);

        assertEquals("INTERNAL_SEARCH", response.getStatus());
        verify(eventProducer, times(1)).publishApproved(any(Demand.class));

        ArgumentCaptor<AuditLogPayload> auditCap = ArgumentCaptor.forClass(AuditLogPayload.class);
        verify(auditLogClient).logAction(auditCap.capture());
        assertTrue(auditCap.getValue().getEndpoint().contains("/api/project-manager/demands/7/approve"));
    }

    @Test
    void approve_whenUserIsProjectManager_sameAsPmRoute_usesDemandsAuditEndpoint() {
        login(42L);
        Demand demand = pendingDemand(7L, 100L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(demand));
        when(userAuthServiceClient.getProjectById(100L)).thenReturn(
                ProjectDto.builder().id(100L).projectManagerId(42L).build());
        doNothing().when(transitionValidator).validate(any(Demand.class), any(DemandStatus.class), any());
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder()
                    .demandId(saved.getDemandId())
                    .status(saved.getStatus().name())
                    .build();
        });

        ApprovalRequest body = ApprovalRequest.builder()
                .decision(DemandStatus.APPROVED)
                .build();

        DemandResponse response = lifecycleService.approve(7L, body);

        assertEquals("INTERNAL_SEARCH", response.getStatus());
        ArgumentCaptor<AuditLogPayload> auditCap = ArgumentCaptor.forClass(AuditLogPayload.class);
        verify(auditLogClient).logAction(auditCap.capture());
        assertTrue(auditCap.getValue().getEndpoint().contains("/api/demands/7/approve"));
    }

    @Test
    void approveAsProjectManager_whenDifferentProjectManager_throwsAccessDenied() {
        login(1L);
        Demand demand = pendingDemand(7L, 100L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(demand));
        when(userAuthServiceClient.getProjectById(100L)).thenReturn(
                ProjectDto.builder().id(100L).projectManagerId(99L).build());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> lifecycleService.approveAsProjectManager(7L, null));
        assertTrue(ex.getMessage().contains("not the project manager"));
        verifyNoInteractions(eventProducer);
    }

    @Test
    void approveAsProjectManager_whenDecisionNotApproved_throwsIllegalArgument() {
        login(42L);
        Demand demand = pendingDemand(7L, 100L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(demand));
        when(userAuthServiceClient.getProjectById(100L)).thenReturn(
                ProjectDto.builder().id(100L).projectManagerId(42L).build());

        ApprovalRequest body = ApprovalRequest.builder()
                .decision(DemandStatus.DRAFT)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> lifecycleService.approveAsProjectManager(7L, body));
        verifyNoInteractions(transitionValidator);
    }

    @Test
    void approveAsProjectManager_nullBody_defaultsToApproved() {
        login(42L);
        Demand demand = pendingDemand(7L, 100L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(demand));
        when(userAuthServiceClient.getProjectById(100L)).thenReturn(
                ProjectDto.builder().id(100L).projectManagerId(42L).build());
        doNothing().when(transitionValidator).validate(any(Demand.class), any(DemandStatus.class), any());
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder()
                    .demandId(saved.getDemandId())
                    .status(saved.getStatus().name())
                    .build();
        });

        DemandResponse response = lifecycleService.approveAsProjectManager(7L, null);

        assertEquals("INTERNAL_SEARCH", response.getStatus());
        verify(eventProducer).publishApproved(any(Demand.class));
    }
}
