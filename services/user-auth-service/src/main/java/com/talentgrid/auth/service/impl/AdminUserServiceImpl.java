package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.request.AdminCreateUserRequest;
import com.talentgrid.auth.dto.request.AdminUpdateUserRequest;
import com.talentgrid.auth.dto.request.AdminPatchUserRequest;
import com.talentgrid.auth.dto.request.ChangeRoleRequest;
import com.talentgrid.auth.dto.response.AdminUserResponse;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.kafka.UserCreatedEventPublisher;
import com.talentgrid.auth.mapper.UserMapper;
import com.talentgrid.auth.entity.Project;
import com.talentgrid.auth.repository.ProjectRepository;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.repository.RefreshTokenRepository;
import com.talentgrid.auth.repository.UserAccountAssignmentRepository;
import com.talentgrid.auth.service.RefreshTokenService;
import com.talentgrid.auth.service.interfaces.AdminUserService;
import com.talentgrid.auth.service.interfaces.UserSecurityCacheService;
import com.talentgrid.auth.kafka.AuthUserEventPublisher;
import com.talentgrid.auth.service.interfaces.UserSecurityRefreshService;
import com.talentgrid.kafka.events.auth.UserCreatedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of AdminUserService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProjectRepository projectRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserAccountAssignmentRepository userAccountAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserSecurityRefreshService userSecurityRefreshService;
    private final RefreshTokenService refreshTokenService;
    private final UserSecurityCacheService userSecurityCacheService;
    private final UserCreatedEventPublisher userCreatedEventPublisher;
    private final AuthUserEventPublisher authUserEventPublisher;

    @Override
    @Transactional
    public AdminUserResponse createUser(AdminCreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        Role.requireAllowedName(request.getRole());

        Role role = roleRepository.findByName(request.getRole().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRole()));

        Long projectId = request.getProjectId();
        if (projectId == null) {
            projectId = projectRepository.findAll().stream()
                    .map(Project::getId)
                    .findFirst()
                    .orElse(null);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .location(request.getLocation())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .slackId(request.getSlackId())
                .roles(Set.of(role))
                .failedAttempts(0)
                .accountLocked(false)
                .projectId(projectId)
                .build();

        User savedUser = userRepository.save(user);

        if (projectId != null && "PORTFOLIO_MANAGER".equalsIgnoreCase(role.getName())) {
            projectRepository.findById(projectId).ifPresent(p -> {
                p.setProjectManagerId(savedUser.getId());
                projectRepository.save(p);
            });
        }

        String projectName = null;
        if (projectId != null) {
            projectName = projectRepository.findById(projectId)
                    .map(Project::getName)
                    .orElse(null);
        }

        UserCreatedPayload payload = UserCreatedPayload.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRoles().stream()
                        .map(Role::getName)
                        .findFirst()
                        .orElse("EMPLOYEE"))
                .location(savedUser.getLocation())
                .projectName(projectName)
                .source("ADMIN_CREATED")
                .build();

        userCreatedEventPublisher.publishUserCreated(payload);

        return userMapper.toAdminUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(Long id, AdminUpdateUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        user.setUsername(request.getUsername());
        user.setLocation(request.getLocation());
        user.setSlackId(request.getSlackId());

        User savedUser = userRepository.save(user);

        User updatedUser = userSecurityRefreshService.refreshUser(savedUser);

        return userMapper.toAdminUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Clear project manager reference on managed projects
        projectRepository.findByProjectManagerId(user.getId()).forEach(p -> {
            p.setProjectManagerId(null);
            projectRepository.save(p);
        });

        // 2. Revoke refresh tokens and delete them
        refreshTokenService.revokeRefreshToken(user);
        refreshTokenRepository.deleteByUser(user);

        // 3. Delete user account assignments
        userAccountAssignmentRepository.deleteByUserId(user.getId());

        // 4. Evict user context from security cache
        userSecurityCacheService.evictUser(user.getId());

        // 5. Publish USER_DELETED event
        authUserEventPublisher.publishUserDeleted(user.getId());

        // 6. Hard-delete user
        userRepository.delete(user);

        log.info("User {} hard-deleted and assignments/sessions cleaned up.", user.getEmail());
    }

    @Override
    public AdminUserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userMapper.toAdminUserResponse(user);
    }

    @Override
    public List<AdminUserResponse> getAllUsers() {

        return userRepository.findAll().stream()
                .map(userMapper::toAdminUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdminUserResponse changeUserRole(Long id, ChangeRoleRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role.requireAllowedName(request.getRoleName());

        Role role = roleRepository.findByName(request.getRoleName().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRoleName()));

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().clear();
        user.getRoles().add(role);

        User savedUser = userRepository.save(user);

        User updatedUser = userSecurityRefreshService.refreshUser(savedUser);

        return userMapper.toAdminUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public AdminUserResponse toggleUserStatus(Long id, boolean enabled) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEnabled(enabled);

        // If locking out the user, revoke refresh tokens
        if (!enabled) {
            refreshTokenService.revokeRefreshToken(user);
        }

        User savedUser = userRepository.save(user);

        User updatedUser = userSecurityRefreshService.refreshUser(savedUser);

        return userMapper.toAdminUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public AdminUserResponse patchUser(Long id, AdminPatchUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getUsername() != null) {
            if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getLocation() != null) {
            user.setLocation(request.getLocation().isEmpty() ? null : request.getLocation());
        }

        if (request.getSlackId() != null) {
            user.setSlackId(request.getSlackId().isEmpty() ? null : request.getSlackId());
        }

        if (request.getRole() != null) {
            Role.requireAllowedName(request.getRole());
            Role role = roleRepository.findByName(request.getRole().toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRole()));
            if (user.getRoles() == null) {
                user.setRoles(new HashSet<>());
            }
            user.getRoles().clear();
            user.getRoles().add(role);
        }

        if (request.getProjectId() != null) {
            Long newProjectId = request.getProjectId();
            String roleName = (request.getRole() != null) ? request.getRole().toUpperCase() : user.getRoles().stream().map(Role::getName).findFirst().orElse("");
            if ("PORTFOLIO_MANAGER".equalsIgnoreCase(roleName)) {
                projectRepository.findByProjectManagerId(user.getId()).forEach(p -> {
                    p.setProjectManagerId(null);
                    projectRepository.save(p);
                });
                projectRepository.findById(newProjectId).ifPresent(p -> {
                    p.setProjectManagerId(user.getId());
                    projectRepository.save(p);
                });
            }
            user.setProjectId(newProjectId);
        }

        User savedUser = userRepository.save(user);
        User updatedUser = userSecurityRefreshService.refreshUser(savedUser);
        return userMapper.toAdminUserResponse(updatedUser);
    }
}
