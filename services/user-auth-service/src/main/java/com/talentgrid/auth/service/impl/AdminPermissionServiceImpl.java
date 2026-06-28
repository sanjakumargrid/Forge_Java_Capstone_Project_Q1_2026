package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.request.UpdateRolePermissionsRequest;
import com.talentgrid.auth.dto.response.PermissionResponse;
import com.talentgrid.auth.dto.response.RolePermissionResponse;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.Scope;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.ScopeRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.interfaces.AdminPermissionService;
import com.talentgrid.auth.service.interfaces.UserSecurityRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPermissionServiceImpl implements AdminPermissionService {

    private final ScopeRepository scopeRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserSecurityRefreshService userSecurityRefreshService;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return scopeRepository.findAll().stream()
                .sorted(Comparator.comparing(Scope::getName))
                .map(scope -> PermissionResponse.builder()
                        .id(scope.getId())
                        .name(scope.getName())
                        .description(scope.getDescription())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RolePermissionResponse getRolePermissions(Long roleId) {
        Role role = findRole(roleId);
        return toRolePermissionResponse(role);
    }

    @Override
    @Transactional
    public RolePermissionResponse updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request) {
        Role role = findRole(roleId);
        Role.requireAllowedName(role.getName());

        Set<String> requested = request.getPermissions().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        List<Scope> scopes = scopeRepository.findByNameIn(requested);
        if (scopes.size() != requested.size()) {
            Set<String> found = scopes.stream().map(Scope::getName).collect(Collectors.toSet());
            Set<String> missing = new HashSet<>(requested);
            missing.removeAll(found);
            throw new IllegalArgumentException("Unknown permissions: " + missing);
        }

        role.setScopes(new HashSet<>(scopes));
        Role savedRole = roleRepository.save(role);

        List<User> affectedUsers = userRepository.findByRolesContaining(savedRole);
        userSecurityRefreshService.refreshUsers(affectedUsers);

        return toRolePermissionResponse(savedRole);
    }

    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
    }

    private RolePermissionResponse toRolePermissionResponse(Role role) {
        Set<String> permissions = role.getScopes().stream()
                .map(Scope::getName)
                .sorted()
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        return RolePermissionResponse.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .permissions(permissions)
                .build();
    }
}
