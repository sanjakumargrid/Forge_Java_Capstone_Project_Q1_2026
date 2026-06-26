package com.talentgrid.demand.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.demand.client.UserAuthServiceClient;
import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandSkill;
import com.talentgrid.demand.domain.entity.Skill;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.dto.request.DemandRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DemandServiceSkillUpdateTest {

    @Mock private DemandRepository demandRepository;
    @Mock private DemandStatusHistoryRepository demandStatusHistoryRepository;
    @Mock private DemandMapper demandMapper;
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
        JwtPrincipal principal = JwtPrincipal.builder().userId(99L).email("test@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));

        demand = new Demand();
        demand.setDemandId(3L);
        demand.setTitle("Test Demand");
        demand.setStatus(DemandStatus.DRAFT);
        demand.setDemandSkills(new ArrayList<>(List.of(mandatorySkill(1L), optionalSkill(10L), optionalSkill(11L))));

        lenient().when(demandRepository.findByDemandIdAndIsDeletedFalse(3L)).thenReturn(Optional.of(demand));
        lenient().when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(demandMapper.toResponse(any())).thenReturn(new DemandResponse());
        lenient().when(skillLookupService.resolveSkillIds(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return ids.stream().map(this::skill).toList();
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateDemand_resubmitsSameSkillIds_replacesCollectionWithoutRepositoryBulkDelete() {
        DemandRequest request = updateRequest();
        request.setMandatorySkillIds(List.of(1L));
        request.setOptionalSkillIds(List.of(10L, 11L));

        demandService.updateDemand(3L, request);

        ArgumentCaptor<Demand> captor = ArgumentCaptor.forClass(Demand.class);
        verify(demandRepository).save(captor.capture());

        List<DemandSkill> skills = captor.getValue().getDemandSkills();
        assertEquals(3, skills.size());
        assertEquals(3, skills.stream().map(ds -> ds.getSkill().getSkillId()).distinct().count());
        assertTrue(skills.stream().anyMatch(ds -> ds.getSkill().getSkillId() == 1L && Boolean.TRUE.equals(ds.getIsMandatory())));
        assertTrue(skills.stream().anyMatch(ds -> ds.getSkill().getSkillId() == 10L && !Boolean.TRUE.equals(ds.getIsMandatory())));
        assertTrue(skills.stream().anyMatch(ds -> ds.getSkill().getSkillId() == 11L && !Boolean.TRUE.equals(ds.getIsMandatory())));
    }

    @Test
    void updateDemand_partialOptionalPatch_mergesMandatoryFromLoadedCollection() {
        DemandRequest request = updateRequest();
        request.setOptionalSkillIds(List.of(10L, 12L));

        demandService.updateDemand(3L, request);

        ArgumentCaptor<Demand> captor = ArgumentCaptor.forClass(Demand.class);
        verify(demandRepository).save(captor.capture());

        List<DemandSkill> skills = captor.getValue().getDemandSkills();
        assertEquals(3, skills.size());
        assertTrue(skills.stream().anyMatch(ds -> ds.getSkill().getSkillId() == 1L && Boolean.TRUE.equals(ds.getIsMandatory())));
        assertTrue(skills.stream().anyMatch(ds -> ds.getSkill().getSkillId() == 10L && !Boolean.TRUE.equals(ds.getIsMandatory())));
        assertTrue(skills.stream().anyMatch(ds -> ds.getSkill().getSkillId() == 12L && !Boolean.TRUE.equals(ds.getIsMandatory())));
    }

    @Test
    void updateDemand_replacesSkillsViaCollectionSync() {
        DemandRequest request = updateRequest();
        request.setMandatorySkillIds(List.of(4L));
        request.setOptionalSkillIds(List.of(5L));

        demandService.updateDemand(3L, request);

        ArgumentCaptor<Demand> captor = ArgumentCaptor.forClass(Demand.class);
        verify(demandRepository).save(captor.capture());

        List<DemandSkill> skills = captor.getValue().getDemandSkills();
        assertEquals(2, skills.size());
        assertTrue(skills.stream().noneMatch(ds -> ds.getSkill().getSkillId() == 1L));
        assertTrue(skills.stream().anyMatch(ds -> ds.getSkill().getSkillId() == 4L && Boolean.TRUE.equals(ds.getIsMandatory())));
        assertTrue(skills.stream().anyMatch(ds -> ds.getSkill().getSkillId() == 5L && !Boolean.TRUE.equals(ds.getIsMandatory())));
    }

    @Test
    void updateDemand_overlapBetweenMandatoryAndOptional_returnsValidationError() {
        DemandRequest request = updateRequest();
        request.setMandatorySkillIds(List.of(1L));
        request.setOptionalSkillIds(List.of(1L, 2L));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> demandService.updateDemand(3L, request));
        assertTrue(ex.getMessage().contains("both mandatorySkillIds and optionalSkillIds"));
        verify(demandRepository, never()).save(any());
    }

    private DemandRequest updateRequest() {
        DemandRequest request = new DemandRequest();
        request.setReasonForEdit("Skill update for unit test");
        return request;
    }

    private DemandSkill mandatorySkill(long skillId) {
        return demandSkill(skillId, true);
    }

    private DemandSkill optionalSkill(long skillId) {
        return demandSkill(skillId, false);
    }

    private DemandSkill demandSkill(long skillId, boolean mandatory) {
        DemandSkill ds = new DemandSkill();
        ds.setId(skillId);
        ds.setDemand(demand);
        ds.setSkill(skill(skillId));
        ds.setIsMandatory(mandatory);
        return ds;
    }

    private Skill skill(long skillId) {
        Skill skill = new Skill();
        skill.setSkillId(skillId);
        skill.setSkillName("Skill-" + skillId);
        return skill;
    }
}
