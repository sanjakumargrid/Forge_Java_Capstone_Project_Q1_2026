package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.response.RoleResponse;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.service.interfaces.AdminRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .scopeCount(role.getScopes().size())
                        .build())
                .toList();
    }
}
