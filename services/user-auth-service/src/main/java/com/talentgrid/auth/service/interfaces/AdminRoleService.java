package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.dto.response.RoleResponse;

import java.util.List;

public interface AdminRoleService {

    List<RoleResponse> getAllRoles();
}
