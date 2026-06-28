package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.request.UpdateRolePermissionsRequest;
import com.talentgrid.auth.dto.response.RolePermissionResponse;
import com.talentgrid.auth.service.interfaces.AdminPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/roles/{roleId}/permissions")
@RequiredArgsConstructor
public class AdminRolePermissionController {

    private final AdminPermissionService adminPermissionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERMISSION_VIEW')")
    public ResponseEntity<RolePermissionResponse> getRolePermissions(@PathVariable Long roleId) {
        return ResponseEntity.ok(adminPermissionService.getRolePermissions(roleId));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERMISSION_ASSIGN')")
    public ResponseEntity<RolePermissionResponse> updateRolePermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateRolePermissionsRequest request
    ) {
        return ResponseEntity.ok(adminPermissionService.updateRolePermissions(roleId, request));
    }
}
