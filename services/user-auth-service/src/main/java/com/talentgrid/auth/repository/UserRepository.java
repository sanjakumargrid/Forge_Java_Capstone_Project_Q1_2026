package com.talentgrid.auth.repository;

import com.talentgrid.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findFirstByLocationAndRoles_NameAndEnabledTrue(
            String location,
            String roleName);

    java.util.List<User> findByLocationAndIsInterviewerEligible(String location, Boolean isInterviewerEligible);
}