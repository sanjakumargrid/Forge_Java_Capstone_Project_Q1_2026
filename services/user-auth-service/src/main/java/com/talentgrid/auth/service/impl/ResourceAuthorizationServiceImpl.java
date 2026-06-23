package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.constants.RoleConstants;
import com.talentgrid.auth.repository.AccountRepository;
import com.talentgrid.auth.repository.ProjectRepository;
import com.talentgrid.auth.security.CachedUserPrincipal;
import com.talentgrid.auth.service.interfaces.ResourceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Database-backed authorization checks for account and project resources.
 */
@Service("resourceAuthorizationService")
@RequiredArgsConstructor
public class ResourceAuthorizationServiceImpl implements ResourceAuthorizationService {

    private final AccountRepository accountRepository;
    private final ProjectRepository projectRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CachedUserPrincipal cachedUserPrincipal) {
            return cachedUserPrincipal.getUserId();
        }

        throw new AccessDeniedException("Unable to resolve current user identity");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        String adminAuthority = RoleConstants.toAuthority(RoleConstants.ADMIN);
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(adminAuthority::equals);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAccountManager(Long accountId) {
        if (accountId == null) {
            return false;
        }

        return accountRepository.findById(accountId)
                .map(account -> Objects.equals(account.getAccountManagerId(), getCurrentUserId()))
                .orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canManageAccount(Long accountId) {
        return isAdmin() || isAccountManager(accountId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canManageProject(Long projectId) {
        if (isAdmin()) {
            return true;
        }

        if (projectId == null) {
            return false;
        }

        return projectRepository.findById(projectId)
                .map(project -> project.getAccount() != null
                        && isAccountManager(project.getAccount().getId()))
                .orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requireCanManageAccount(Long accountId) {
        if (!canManageAccount(accountId)) {
            throw new AccessDeniedException("You do not have permission to manage this account");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requireCanManageProject(Long projectId) {
        if (!canManageProject(projectId)) {
            throw new AccessDeniedException("You do not have permission to manage this project");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Administrator privileges required");
        }
    }
}
