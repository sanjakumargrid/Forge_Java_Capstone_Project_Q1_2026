package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.dto.request.UpdateRolePermissionsRequest;
import com.talentgrid.auth.dto.response.PermissionResponse;
import com.talentgrid.auth.dto.response.RolePermissionResponse;

import java.util.List;

public interface AdminPermissionService {

    List<PermissionResponse> getAllPermissions();

    RolePermissionResponse getRolePermissions(Long roleId);

    RolePermissionResponse updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request);
}
