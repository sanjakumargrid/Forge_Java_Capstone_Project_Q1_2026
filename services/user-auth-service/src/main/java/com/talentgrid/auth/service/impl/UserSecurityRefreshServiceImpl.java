package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.Scope;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.kafka.AuthUserEventPublisher;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.interfaces.UserSecurityCacheService;
import com.talentgrid.auth.service.interfaces.UserSecurityRefreshService;
import com.talentgrid.kafka.events.auth.AuthUserPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSecurityRefreshServiceImpl implements UserSecurityRefreshService {

    private final UserRepository userRepository;
    private final UserSecurityCacheService userSecurityCacheService;
    private final AuthUserEventPublisher authUserEventPublisher;

    @Override
    @Transactional
    public User refreshUser(User user) {
        Long currentVersion = user.getAuthVersion();
        user.setAuthVersion(currentVersion == null ? 1L : currentVersion + 1);
        User savedUser = userRepository.save(user);
        userSecurityCacheService.cacheUser(savedUser);
        publishUpdate(savedUser);
        return savedUser;
    }

    @Override
    @Transactional
    public void refreshUsers(Collection<User> users) {
        users.forEach(this::refreshUser);
    }

    private void publishUpdate(User user) {
        AuthUserPayload payload = AuthUserPayload.builder()
                .userId(user.getId())
                .authVersion(user.getAuthVersion())
                .enabled(user.getEnabled())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .scopes(user.getRoles().stream()
                        .flatMap(role -> role.getScopes().stream())
                        .map(Scope::getName)
                        .collect(Collectors.toSet()))
                .build();
        authUserEventPublisher.publishUserUpdated(payload);
    }
}
