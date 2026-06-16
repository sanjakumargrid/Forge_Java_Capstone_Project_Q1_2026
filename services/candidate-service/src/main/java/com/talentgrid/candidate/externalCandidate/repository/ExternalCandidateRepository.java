package com.talentgrid.candidate.externalCandidate.repository;

import com.talentgrid.candidate.externalCandidate.entity.ExternalCandidate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExternalCandidateRepository extends JpaRepository<ExternalCandidate, Long> {

    @Query("""
            SELECT c FROM ExternalCandidate c
            WHERE c.isDeleted = false
            AND (c.emailHash = :emailHash OR c.phoneHash = :phoneHash)
            """)
    Optional<ExternalCandidate> findActiveDuplicate(
            @Param("emailHash") String emailHash,
            @Param("phoneHash") String phoneHash
    );

    @Query("""
            SELECT c FROM ExternalCandidate c
            WHERE c.isDeleted = false
            AND c.candidateId <> :candidateId
            AND (c.emailHash = :emailHash OR c.phoneHash = :phoneHash)
            """)
    Optional<ExternalCandidate> findDuplicateForUpdate(
            @Param("candidateId") Long candidateId,
            @Param("emailHash") String emailHash,
            @Param("phoneHash") String phoneHash
    );

    Optional<ExternalCandidate> findByCandidateIdAndIsDeletedFalse(Long candidateId);

    @EntityGraph(attributePaths = {
            "skills",
            "educationDetails",
            "certificationDetails"
    })
    Optional<ExternalCandidate> findWithDetailsByCandidateIdAndIsDeletedFalse(Long candidateId);
}