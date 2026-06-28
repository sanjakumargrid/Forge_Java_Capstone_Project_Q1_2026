package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.response.RoleResponse;
import com.talentgrid.auth.service.interfaces.AdminRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_VIEW')")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(adminRoleService.getAllRoles());
    }
}
