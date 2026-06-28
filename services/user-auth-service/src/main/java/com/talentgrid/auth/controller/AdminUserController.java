package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.request.AdminCreateUserRequest;
import com.talentgrid.auth.dto.request.AdminUpdateUserRequest;
import com.talentgrid.auth.dto.request.ChangeRoleRequest;
import com.talentgrid.auth.dto.request.UserStatusRequest;
import com.talentgrid.auth.dto.response.AdminUserResponse;
import com.talentgrid.auth.service.interfaces.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Admin User Management.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Expose user management endpoints for administrators</li>
 *   <li>Ensure only authorized users (ADMIN role or specific permissions) can access</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_CREATE')")
    public ResponseEntity<AdminUserResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest request
    ) {
        return ResponseEntity.ok(adminUserService.createUser(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_UPDATE')")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id
    ) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_VIEW')")
    public ResponseEntity<AdminUserResponse> getUserById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_VIEW')")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_ASSIGN')")
    public ResponseEntity<AdminUserResponse> assignUserRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        return ResponseEntity.ok(adminUserService.changeUserRole(id, request));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_ASSIGN')")
    public ResponseEntity<AdminUserResponse> changeUserRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        return ResponseEntity.ok(adminUserService.changeUserRole(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_UPDATE')")
    public ResponseEntity<AdminUserResponse> toggleUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        return ResponseEntity.ok(adminUserService.toggleUserStatus(id, request.getEnabled()));
    }
}
