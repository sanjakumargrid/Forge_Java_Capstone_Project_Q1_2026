package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.dto.cache.CachedUserContext;
import com.talentgrid.auth.entity.User;

/**
 * Service interface for managing the user security cache in Redis.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Cache user roles, scopes, and authVersion upon login</li>
 *   <li>Provide fast O(1) lookups during JWT validation</li>
 *   <li>Evict cache when required</li>
 * </ul>
 */
public interface UserSecurityCacheService {

    /**
     * Caches the user's authorization state in Redis.
     *
     * @param user the authenticated user entity
     */
    void cacheUser(User user);

    /**
     * Retrieves the user's authorization state from Redis.
     *
     * @param userId the user's database identifier
     * @return the cached context, or null if missing
     */
    CachedUserContext getUser(Long userId);

    /**
     * Removes the user's authorization state from Redis.
     *
     * @param userId the user's database identifier
     */
    void evictUser(Long userId);

}