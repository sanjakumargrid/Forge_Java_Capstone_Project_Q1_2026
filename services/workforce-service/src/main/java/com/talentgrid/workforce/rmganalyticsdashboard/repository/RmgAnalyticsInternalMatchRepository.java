package com.talentgrid.workforce.rmganalyticsdashboard.repository;

import com.talentgrid.workforce.rmgnomination.entity.InternalMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Analytics-only queries for {@link InternalMatch} (keeps nomination package repositories unchanged).
 */
@Repository
public interface RmgAnalyticsInternalMatchRepository extends JpaRepository<InternalMatch, Long> {

    @Query("SELECT COUNT(m) FROM InternalMatch m WHERE m.isDeleted = false AND m.nominatedAt >= :start AND m.nominatedAt < :end")
    long countNominationsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(m) FROM InternalMatch m WHERE m.isDeleted = false AND m.matchStatus = 'ACCEPTED'")
    long countAcceptedNonDeleted();

    @Query("SELECT COUNT(m) FROM InternalMatch m WHERE m.isDeleted = false AND m.matchStatus = 'REJECTED'")
    long countRejectedNonDeleted();

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (reviewed_at - nominated_at)) / 86400.0) FROM internal_matches "
            + "WHERE is_deleted = false AND match_status IN ('ACCEPTED','REJECTED') AND reviewed_at IS NOT NULL",
            nativeQuery = true)
    Double averageNominationToDecisionDays();

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (reviewed_at - nominated_at)) / 86400.0) FROM internal_matches "
            + "WHERE is_deleted = false AND match_status IN ('ACCEPTED','REJECTED') AND reviewed_at IS NOT NULL "
            + "AND nominated_at >= :since",
            nativeQuery = true)
    Double averageNominationToDecisionDaysSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(m) FROM InternalMatch m WHERE m.isDeleted = false AND m.matchStatus IN ('ACCEPTED','REJECTED') AND m.nominatedAt >= :since")
    long countDecidedSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(m) FROM InternalMatch m WHERE m.isDeleted = false AND m.matchStatus = 'ACCEPTED' AND m.nominatedAt >= :since")
    long countAcceptedSince(@Param("since") LocalDateTime since);
}
