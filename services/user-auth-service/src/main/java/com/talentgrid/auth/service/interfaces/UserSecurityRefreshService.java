package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.entity.User;

import java.util.Collection;

/**
 * Bumps {@code authVersion}, refreshes Redis cache, and publishes auth update events.
 */
public interface UserSecurityRefreshService {

    User refreshUser(User user);

    void refreshUsers(Collection<User> users);
}
