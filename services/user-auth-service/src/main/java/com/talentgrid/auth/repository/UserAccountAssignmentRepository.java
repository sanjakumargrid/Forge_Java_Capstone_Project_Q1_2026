package com.talentgrid.auth.repository;

import com.talentgrid.auth.entity.UserAccountAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence layer for user-to-account (and optional project) assignments.
 */
@Repository
public interface UserAccountAssignmentRepository extends JpaRepository<UserAccountAssignment, Long> {

    /**
     * @param userId user id
     * @return assignments for the user
     */
    List<UserAccountAssignment> findByUserId(Long userId);

    /**
     * @param accountId account id
     * @return assignments for the account
     */
    List<UserAccountAssignment> findByAccountId(Long accountId);

    /**
     * @param userId    user id
     * @param accountId account id
     * @return account-level assignment without a project
     */
    Optional<UserAccountAssignment> findByUserIdAndAccountIdAndProjectIsNull(Long userId, Long accountId);

    /**
     * @param userId    user id
     * @param accountId account id
     * @param projectId project id
     * @return assignment for the exact user/account/project tuple
     */
    Optional<UserAccountAssignment> findByUserIdAndAccountIdAndProjectId(Long userId, Long accountId, Long projectId);

    /**
     * @param userId    user id
     * @param accountId account id
     * @return whether an account-level assignment already exists
     */
    boolean existsByUserIdAndAccountIdAndProjectIsNull(Long userId, Long accountId);

    /**
     * @param userId    user id
     * @param accountId account id
     * @param projectId project id
     * @return whether the assignment tuple already exists
     */
    boolean existsByUserIdAndAccountIdAndProjectId(Long userId, Long accountId, Long projectId);

    void deleteByUserId(Long userId);
}
