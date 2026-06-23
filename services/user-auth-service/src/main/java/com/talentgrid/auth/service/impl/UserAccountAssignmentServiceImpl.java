package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.request.CreateUserAccountAssignmentRequest;
import com.talentgrid.auth.dto.request.UpdateUserAccountAssignmentRequest;
import com.talentgrid.auth.dto.response.UserAccountAssignmentResponse;
import com.talentgrid.auth.entity.Account;
import com.talentgrid.auth.entity.Project;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.entity.UserAccountAssignment;
import com.talentgrid.auth.exception.ResourceNotFoundException;
import com.talentgrid.auth.mapper.UserAccountAssignmentMapper;
import com.talentgrid.auth.repository.AccountRepository;
import com.talentgrid.auth.repository.ProjectRepository;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.UserAccountAssignmentRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.interfaces.ResourceAuthorizationService;
import com.talentgrid.auth.service.interfaces.UserAccountAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link UserAccountAssignmentService} restricted to administrators.
 */
@Service
@RequiredArgsConstructor
public class UserAccountAssignmentServiceImpl implements UserAccountAssignmentService {

    private final UserAccountAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ProjectRepository projectRepository;
    private final RoleRepository roleRepository;
    private final UserAccountAssignmentMapper assignmentMapper;
    private final ResourceAuthorizationService resourceAuthorizationService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserAccountAssignmentResponse createAssignment(CreateUserAccountAssignmentRequest request) {
        resourceAuthorizationService.requireAdmin();

        User user = findUserOrThrow(request.getUserId());
        Account account = findAccountOrThrow(request.getAccountId());
        Project project = resolveProject(request.getProjectId(), request.getAccountId());

        assertAssignmentUnique(request.getUserId(), request.getAccountId(), request.getProjectId(), null);
        applyRoleIfPresent(user, request.getRoleName());

        UserAccountAssignment assignment = UserAccountAssignment.builder()
                .user(user)
                .account(account)
                .project(project)
                .build();

        return assignmentMapper.toResponse(assignmentRepository.save(assignment));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserAccountAssignmentResponse getAssignmentById(Long id) {
        resourceAuthorizationService.requireAdmin();
        return assignmentMapper.toResponse(findAssignmentOrThrow(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserAccountAssignmentResponse> getAllAssignments() {
        resourceAuthorizationService.requireAdmin();
        return assignmentRepository.findAll().stream()
                .map(assignmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserAccountAssignmentResponse updateAssignment(Long id, UpdateUserAccountAssignmentRequest request) {
        resourceAuthorizationService.requireAdmin();

        UserAccountAssignment assignment = findAssignmentOrThrow(id);
        User user = findUserOrThrow(request.getUserId());
        Account account = findAccountOrThrow(request.getAccountId());
        Project project = resolveProject(request.getProjectId(), request.getAccountId());

        assertAssignmentUnique(request.getUserId(), request.getAccountId(), request.getProjectId(), id);
        applyRoleIfPresent(user, request.getRoleName());

        assignment.setUser(user);
        assignment.setAccount(account);
        assignment.setProject(project);

        return assignmentMapper.toResponse(assignmentRepository.save(assignment));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteAssignment(Long id) {
        resourceAuthorizationService.requireAdmin();
        UserAccountAssignment assignment = findAssignmentOrThrow(id);
        assignmentRepository.delete(assignment);
    }

    private UserAccountAssignment findAssignmentOrThrow(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    private Project resolveProject(Long projectId, Long accountId) {
        if (projectId == null) {
            return null;
        }

        Project project = projectRepository.findByIdWithAccount(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (project.getAccount() == null || !project.getAccount().getId().equals(accountId)) {
            throw new RuntimeException("Project " + projectId + " does not belong to account " + accountId);
        }

        return project;
    }

    private void assertAssignmentUnique(Long userId, Long accountId, Long projectId, Long currentAssignmentId) {
        boolean exists;
        if (projectId == null) {
            exists = assignmentRepository.existsByUserIdAndAccountIdAndProjectIsNull(userId, accountId);
        } else {
            exists = assignmentRepository.existsByUserIdAndAccountIdAndProjectId(userId, accountId, projectId);
        }

        if (exists) {
            UserAccountAssignment existing = projectId == null
                    ? assignmentRepository.findByUserIdAndAccountIdAndProjectIsNull(userId, accountId).orElse(null)
                    : assignmentRepository.findByUserIdAndAccountIdAndProjectId(userId, accountId, projectId).orElse(null);

            if (existing != null && (currentAssignmentId == null || !existing.getId().equals(currentAssignmentId))) {
                throw new RuntimeException("Assignment already exists for the given user, account, and project scope");
            }
        }
    }

    private void applyRoleIfPresent(User user, String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return;
        }

        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
        user.setRoles(Set.of(role));
        userRepository.save(user);
    }
}
