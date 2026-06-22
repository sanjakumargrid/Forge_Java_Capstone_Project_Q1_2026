package com.talentgrid.demand.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.client.dto.ProjectDto;
import com.talentgrid.demand.client.dto.UserDto;
import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.ClosureReason;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.statemachine.TransitionValidator;
import com.talentgrid.demand.dto.request.ApprovalRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.kafka.producer.DemandEventProducer;
import com.talentgrid.demand.mapper.DemandMapper;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.repository.DemandStatusHistoryRepository;
import com.talentgrid.demand.scheduler.ApprovalSlaScheduler;
import com.talentgrid.shared.auth.security.JwtPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
public class ApprovalSlaWorkflowTest {

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
    @Mock
    private ApprovalReminderService approvalReminderService;

    @InjectMocks
    private DemandLifecycleService lifecycleService;

    private ApprovalSlaScheduler slaScheduler;

    @BeforeEach
    void setUp() {
        slaScheduler = new ApprovalSlaScheduler(
                demandRepository,
                historyRepository,
                approvalReminderService,
                userAuthServiceClient,
                eventProducer
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

    private Demand draftDemand(long demandId, long projectId, long creatorId) {
        Demand d = new Demand();
        d.setDemandId(demandId);
        d.setStatus(DemandStatus.DRAFT);
        d.setProjectId(projectId);
        d.setCreatedBy(creatorId);
        d.setIsDeleted(false);
        return d;
    }

    @Test
    void testHmSubmitsDemand_sendsNotificationsToHmAndPm() {
        // Log in as Hiring Manager (creator of demand)
        login(10L, "HIRING_MANAGER");

        Demand demand = draftDemand(1L, 100L, 10L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(demand));
        doNothing().when(transitionValidator).validate(any(Demand.class), any(DemandStatus.class), any());
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        // Mock PM details to verify that PM lookup is triggered
        ProjectDto projectDto = ProjectDto.builder().id(100L).projectManagerId(20L).build();
        UserDto pmDto = UserDto.builder().id(20L).name("PM Manager").email("pm@company.com").slackId("SLACK_PM").build();
        when(userAuthServiceClient.getProjectById(100L)).thenReturn(projectDto);
        when(userAuthServiceClient.getUserById(20L)).thenReturn(pmDto);

        DemandResponse response = lifecycleService.submitDemand(1L, "Please approve ASAP");

        assertEquals("PENDING_APPROVAL", response.getStatus());
        // Verify that PENDING_APPROVAL event was published with PM's resolved info
        verify(eventProducer).publishPendingApproval(eq(demand), eq(20L), eq("PM Manager"), eq("pm@company.com"), eq("SLACK_PM"));
    }

    @Test
    void testPmApproveDemand_allowedOnlyForPm() {
        // Scenario 1: Non-PM tries to approve -> throws AccessDenied
        login(30L, "PROJECT_MANAGER");
        Demand demand = new Demand();
        demand.setDemandId(2L);
        demand.setStatus(DemandStatus.PENDING_APPROVAL);
        demand.setProjectId(100L);
        demand.setIsDeleted(false);

        when(demandRepository.findByDemandIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(demand));
        when(userAuthServiceClient.getProjectById(100L)).thenReturn(
                ProjectDto.builder().id(100L).projectManagerId(20L).build()); // Project PM is 20L

        assertThrows(AccessDeniedException.class, () -> {
            lifecycleService.approveAsProjectManager(2L, ApprovalRequest.builder().decision(DemandStatus.APPROVED).build());
        });

        // Scenario 2: Actual PM approves -> transitions to INTERNAL_SEARCH/APPROVED
        login(20L, "PROJECT_MANAGER");
        doNothing().when(transitionValidator).validate(any(Demand.class), any(DemandStatus.class), any());
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        DemandResponse response = lifecycleService.approveAsProjectManager(2L, ApprovalRequest.builder().decision(DemandStatus.APPROVED).build());
        assertEquals("INTERNAL_SEARCH", response.getStatus());
    }

    @Test
    void testSlaScheduler_24hReminderSentToBothHmAndPm() {
        Demand demand = new Demand();
        demand.setDemandId(3L);
        demand.setStatus(DemandStatus.PENDING_APPROVAL);
        demand.setProjectId(100L);
        demand.setApprovalReminderSent(false);

        when(demandRepository.findByStatusAndIsDeletedFalse(DemandStatus.PENDING_APPROVAL))
                .thenReturn(List.of(demand));

        DemandStatusHistory history = new DemandStatusHistory();
        history.setToStatus(DemandStatus.PENDING_APPROVAL);
        history.setChangedAt(OffsetDateTime.now().minusHours(25)); // pending for 25 hours
        when(historyRepository.findTopByDemandDemandIdAndToStatusOrderByChangedAtDesc(3L, DemandStatus.PENDING_APPROVAL))
                .thenReturn(Optional.of(history));

        // Mock PM resolution
        when(userAuthServiceClient.getProjectById(100L)).thenReturn(
                ProjectDto.builder().id(100L).projectManagerId(20L).build());
        when(userAuthServiceClient.getUserById(20L)).thenReturn(
                UserDto.builder().id(20L).name("PM Manager").email("pm@company.com").slackId("SLACK_PM").build());

        // Stub reminder service to return true (sent)
        when(approvalReminderService.sendApprovalReminder(eq(demand), any(), eq(25L))).thenReturn(true);

        slaScheduler.checkApprovalSla();

        // Verify reminder sent flag was updated in database
        verify(demandRepository).save(demand);
        assertTrue(demand.getApprovalReminderSent());
    }

    @Test
    void testSlaScheduler_72hAutoCloseAndNotifiesBothHmAndPm() {
        Demand demand = new Demand();
        demand.setDemandId(4L);
        demand.setStatus(DemandStatus.PENDING_APPROVAL);
        demand.setProjectId(100L);

        when(demandRepository.findByStatusAndIsDeletedFalse(DemandStatus.PENDING_APPROVAL))
                .thenReturn(List.of(demand));

        DemandStatusHistory history = new DemandStatusHistory();
        history.setToStatus(DemandStatus.PENDING_APPROVAL);
        history.setChangedAt(OffsetDateTime.now().minusHours(73)); // pending for 73 hours
        when(historyRepository.findTopByDemandDemandIdAndToStatusOrderByChangedAtDesc(4L, DemandStatus.PENDING_APPROVAL))
                .thenReturn(Optional.of(history));

        // Mock PM resolution
        when(userAuthServiceClient.getProjectById(100L)).thenReturn(
                ProjectDto.builder().id(100L).projectManagerId(20L).build());
        when(userAuthServiceClient.getUserById(20L)).thenReturn(
                UserDto.builder().id(20L).name("PM Manager").email("pm@company.com").slackId("SLACK_PM").build());

        slaScheduler.checkApprovalSla();

        // Verify demand is closed with correct reason
        assertEquals(DemandStatus.CLOSED, demand.getStatus());
        assertEquals(ClosureReason.SLA_APPROVAL_BREACH.name(), demand.getClosureReason());
        verify(demandRepository).save(demand);
        // Verify SLA breach event was published
        verify(eventProducer).publishApprovalSlaClosed(eq(demand), eq(20L), eq("PM Manager"), eq("pm@company.com"), eq("SLACK_PM"));
    }

    @Test
    void testPmSubmitsDemand_autoApprovesDirectly() {
        // Log in as Project Manager (creator and PM of project)
        login(20L, "PROJECT_MANAGER");

        Demand demand = draftDemand(5L, 100L, 20L);
        when(demandRepository.findByDemandIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(demand));
        when(userAuthServiceClient.getProjectById(100L)).thenReturn(
                ProjectDto.builder().id(100L).projectManagerId(20L).build()); // current user is PM

        doNothing().when(transitionValidator).validate(any(Demand.class), any(DemandStatus.class), any());
        when(demandRepository.save(any(Demand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(demandMapper.toResponse(any(Demand.class))).thenAnswer(inv -> {
            Demand saved = inv.getArgument(0);
            return DemandResponse.builder().demandId(saved.getDemandId()).status(saved.getStatus().name()).build();
        });

        DemandResponse response = lifecycleService.submitDemand(5L, "Auto-approve PM created demand");

        // Verify that PM created demand transitioned directly to INTERNAL_SEARCH/APPROVED
        assertEquals("INTERNAL_SEARCH", response.getStatus());
        verify(eventProducer).publishApproved(any(Demand.class));
    }
}
