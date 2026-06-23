package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.constants.RoleConstants;
import com.talentgrid.auth.entity.Account;
import com.talentgrid.auth.entity.Project;
import com.talentgrid.auth.repository.AccountRepository;
import com.talentgrid.auth.repository.ProjectRepository;
import com.talentgrid.auth.security.CachedUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

/**
 * Unit tests for {@link ResourceAuthorizationServiceImpl}.
 *
 * <p>Verifies that JWT-derived {@code ROLE_*} authorities (from DB roles) and
 * account-manager ownership checks behave as expected.
 */
@ExtendWith(MockitoExtension.class)
class ResourceAuthorizationServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ResourceAuthorizationServiceImpl authorizationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void isAdminReturnsTrueWhenJwtHasAdminRole() {
        authenticate(1L, List.of(RoleConstants.toAuthority(RoleConstants.ADMIN)));
        assertTrue(authorizationService.isAdmin());
    }

    @Test
    void isAdminReturnsFalseForScopeOnlyToken() {
        authenticate(2L, List.of("DEMAND_VIEW", "DEMAND_CREATE"));
        assertFalse(authorizationService.isAdmin());
    }

    @Test
    void isAccountManagerMatchesAccountManagerId() {
        authenticate(10L, List.of("ROLE_HIRING_MANAGER"));
        when(accountRepository.findById(5L)).thenReturn(Optional.of(Account.builder()
                .id(5L)
                .accountManagerId(10L)
                .build()));

        assertTrue(authorizationService.isAccountManager(5L));
        assertTrue(authorizationService.canManageAccount(5L));
    }

    @Test
    void canManageProjectAllowsAdminWithoutDbLookup() {
        authenticate(1L, List.of(RoleConstants.toAuthority(RoleConstants.ADMIN)));
        assertTrue(authorizationService.canManageProject(99L));
    }

    @Test
    void canManageProjectAllowsAccountManagerOfOwningAccount() {
        authenticate(10L, List.of("ROLE_PROJECT_MANAGER"));

        Account account = Account.builder()
                .id(5L)
                .accountManagerId(10L)
                .build();

        Project project = Project.builder()
                .id(7L)
                .account(account)
                .build();

        when(projectRepository.findById(7L))
                .thenReturn(Optional.of(project));

        when(accountRepository.findById(5L))
                .thenReturn(Optional.of(account));

        assertTrue(authorizationService.canManageProject(7L));
    }

  @Test
  void canManageProjectDeniesUnrelatedUser() {
    authenticate(3L, List.of("ROLE_HIRING_MANAGER"));
    Account account = Account.builder().id(5L).accountManagerId(10L).build();
    Project project = Project.builder().id(7L).account(account).build();

    when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

    assertFalse(authorizationService.canManageProject(7L));
  }

    private void authenticate(Long userId, List<String> authorities) {
        CachedUserPrincipal principal = CachedUserPrincipal.builder()
                .userId(userId)
                .email("user@griddynamics.com")
                .enabled(true)
                .authVersion(1L)
                .build();

        List<SimpleGrantedAuthority> granted = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, granted));
    }
}
