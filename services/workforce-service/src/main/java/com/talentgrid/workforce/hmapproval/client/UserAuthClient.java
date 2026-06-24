package com.talentgrid.workforce.hmapproval.client;

import com.talentgrid.workforce.common.config.FeignAuthConfig;
import com.talentgrid.workforce.hmapproval.dto.UserRoleResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-auth-service",
        url = "${user-auth-service.url:http://localhost:8081}",
        configuration = FeignAuthConfig.class
)
public interface UserAuthClient {

    /**
     * Fetches a user's details including their assigned roles.
     * Requires ADMIN role or USER_VIEW authority on the caller's token.
     */
    @GetMapping("/api/v1/admin/users/{id}")
    UserRoleResponse getUserById(@PathVariable("id") Long userId);
}
