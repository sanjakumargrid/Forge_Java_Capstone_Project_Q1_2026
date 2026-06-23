package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.request.CreateProjectRequest;
import com.talentgrid.auth.dto.request.UpdateProjectRequest;
import com.talentgrid.auth.dto.response.ProjectResponse;
import com.talentgrid.auth.entity.Account;
import com.talentgrid.auth.entity.Project;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.exception.ResourceNotFoundException;
import com.talentgrid.auth.mapper.ProjectMapper;
import com.talentgrid.auth.repository.AccountRepository;
import com.talentgrid.auth.repository.ProjectRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.interfaces.ProjectService;
import com.talentgrid.auth.service.interfaces.ResourceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ProjectService} with role- and ownership-based authorization.
 */
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final ResourceAuthorizationService resourceAuthorizationService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        resourceAuthorizationService.requireCanManageAccount(request.getAccountId());

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + request.getAccountId()));

        if (projectRepository.existsByNameAndAccountId(request.getName(), request.getAccountId())) {
            throw new RuntimeException("Project name already exists for this account: " + request.getName());
        }

        if (request.getProjectManagerId() != null) {
            validateProjectManager(request.getProjectManagerId());
        }

        Project project = Project.builder()
                .name(request.getName())
                .account(account)
                .projectManagerId(request.getProjectManagerId())
                .build();

        return projectMapper.toResponse(projectRepository.save(project));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = findProjectOrThrow(id);
        resourceAuthorizationService.requireCanManageProject(project.getId());
        return projectMapper.toResponse(project);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectByIdForLookup(Long id) {
        return projectMapper.toResponse(findProjectOrThrow(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        if (resourceAuthorizationService.isAdmin()) {
            return projectRepository.findAll().stream()
                    .map(projectMapper::toResponse)
                    .collect(Collectors.toList());
        }

        Long currentUserId = resourceAuthorizationService.getCurrentUserId();
        List<Account> managedAccounts = accountRepository.findByAccountManagerId(currentUserId);
        if (managedAccounts.isEmpty()) {
            throw new AccessDeniedException("You do not have permission to view projects");
        }

        List<Long> accountIds = managedAccounts.stream()
                .map(Account::getId)
                .collect(Collectors.toList());

        return projectRepository.findByAccountIdIn(accountIds).stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        Project project = findProjectOrThrow(id);
        resourceAuthorizationService.requireCanManageProject(project.getId());

        Long accountId = project.getAccount().getId();
        if (projectRepository.existsByNameAndAccountIdAndIdNot(request.getName(), accountId, id)) {
            throw new RuntimeException("Project name already exists for this account: " + request.getName());
        }

        if (request.getProjectManagerId() != null) {
            validateProjectManager(request.getProjectManagerId());
        }

        project.setName(request.getName());
        project.setProjectManagerId(request.getProjectManagerId());

        return projectMapper.toResponse(projectRepository.save(project));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteProject(Long id) {
        Project project = findProjectOrThrow(id);
        resourceAuthorizationService.requireCanManageProject(project.getId());
        projectRepository.delete(project);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByProjectManagerId(Long projectManagerId) {
        return projectRepository.findByProjectManagerId(projectManagerId).stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Project findProjectOrThrow(Long id) {
        return projectRepository.findByIdWithAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    private void validateProjectManager(Long projectManagerId) {
        User projectManager = userRepository.findById(projectManagerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project manager user not found with id: " + projectManagerId));

        if (!Boolean.TRUE.equals(projectManager.getEnabled())) {
            throw new RuntimeException("Project manager user is disabled: " + projectManagerId);
        }
    }
}
