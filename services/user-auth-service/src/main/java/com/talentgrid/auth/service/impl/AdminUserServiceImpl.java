package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.request.AdminCreateUserRequest;
import com.talentgrid.auth.dto.request.AdminUpdateUserRequest;
import com.talentgrid.auth.dto.request.ChangeRoleRequest;
import com.talentgrid.auth.dto.response.AdminUserResponse;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.Scope;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.kafka.AuthUserEventPublisher;
import com.talentgrid.auth.kafka.UserCreatedEventPublisher;
import com.talentgrid.auth.mapper.UserMapper;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.RefreshTokenService;
import com.talentgrid.auth.service.interfaces.AdminUserService;
import com.talentgrid.auth.service.interfaces.UserSecurityCacheService;
import com.talentgrid.kafka.events.auth.UserCreatedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.talentgrid.kafka.events.auth.AuthUserPayload;

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
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserSecurityCacheService userSecurityCacheService;
    private final RefreshTokenService refreshTokenService;
    private final AuthUserEventPublisher authUserEventPublisher;
    private final UserCreatedEventPublisher userCreatedEventPublisher;

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
                .build();

        User savedUser = userRepository.save(user);

        UserCreatedPayload payload = UserCreatedPayload.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRoles().stream()
                        .map(Role::getName)
                        .findFirst()
                        .orElse("EMPLOYEE"))
                .location(savedUser.getLocation())
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

        User updatedUser = refreshUserSecurity(savedUser);

        return userMapper.toAdminUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft delete: disable the user
        user.setEnabled(false);

        User savedUser = userRepository.save(user);

        // Revoke refresh tokens
        refreshTokenService.revokeRefreshToken(savedUser);

        // Update cache so enabled=false propagates immediately
        refreshUserSecurity(savedUser);
        
        log.info("User {} soft-deleted and sessions invalidated.", user.getEmail());
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

        user.setRoles(Set.of(role));

        User savedUser = userRepository.save(user);

        User updatedUser = refreshUserSecurity(savedUser);

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

        User updatedUser = refreshUserSecurity(savedUser);

        return userMapper.toAdminUserResponse(updatedUser);
    }

    /**
     * Increments the user's authorization version and updates the Redis cache.
     * This invalidates all previously issued JWTs.
     */
    private User refreshUserSecurity(User user) {

        user.setAuthVersion(user.getAuthVersion() + 1);
        User savedUser = userRepository.save(user);
        userSecurityCacheService.cacheUser(savedUser);

        AuthUserPayload payload = AuthUserPayload.builder()
                .userId(savedUser.getId())
                .authVersion(savedUser.getAuthVersion())
                .enabled(savedUser.getEnabled())
                .roles(savedUser.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .scopes(savedUser.getRoles().stream()
                        .flatMap(role -> role.getScopes().stream())
                        .map(Scope::getName)
                        .collect(Collectors.toSet()))
                .build();

        authUserEventPublisher.publishUserUpdated(payload);
        return savedUser;
    }
}
