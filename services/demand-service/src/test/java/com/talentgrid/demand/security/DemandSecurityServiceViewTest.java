package com.talentgrid.demand.security;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.shared.auth.security.JwtPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandSecurityServiceViewTest {

    @Mock
    private DemandRepository demandRepository;

    private DemandSecurityService demandSecurityService;

    @BeforeEach
    void setUp() {
        demandSecurityService = new DemandSecurityService(demandRepository);
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
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @ParameterizedTest
    @EnumSource(DemandStatus.class)
    void resourceManagerCanViewDemandAtEveryStage(DemandStatus status) {
        login(40L, "RESOURCE_MANAGER");

        Demand demand = new Demand();
        demand.setDemandId(1L);
        demand.setStatus(status);
        demand.setCreatedBy(99L);
        demand.setIsDeleted(false);

        when(demandRepository.findById(1L)).thenReturn(Optional.of(demand));

        assertTrue(demandSecurityService.canView(1L));
    }

    @Test
    void resourceManagerAliasRmgCanViewDraftDemand() {
        login(41L, "RMG");

        Demand demand = new Demand();
        demand.setDemandId(2L);
        demand.setStatus(DemandStatus.DRAFT);
        demand.setCreatedBy(99L);
        demand.setIsDeleted(false);

        when(demandRepository.findById(2L)).thenReturn(Optional.of(demand));

        assertTrue(demandSecurityService.canView(2L));
    }

    @Test
    void recruiterCannotViewDraftDemandUnlessOwner() {
        login(50L, "RECRUITER");

        Demand demand = new Demand();
        demand.setDemandId(3L);
        demand.setStatus(DemandStatus.DRAFT);
        demand.setCreatedBy(99L);
        demand.setIsDeleted(false);

        when(demandRepository.findById(3L)).thenReturn(Optional.of(demand));

        assertFalse(demandSecurityService.canView(3L));
    }
}
