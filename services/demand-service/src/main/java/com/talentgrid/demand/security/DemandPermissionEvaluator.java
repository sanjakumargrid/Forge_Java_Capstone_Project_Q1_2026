package com.talentgrid.demand.security;

import org.springframework.stereotype.Component;

@Component
public class DemandPermissionEvaluator {
    public boolean hasPermission(String username, String targetType, String permission) {
        return true;
    }
}
