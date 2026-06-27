package com.talentgrid.workforce.rmgnomination.repository;

import com.talentgrid.workforce.rmgnomination.entity.InternalMatch;
import com.talentgrid.workforce.rmgnomination.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternalMatchRepository extends JpaRepository<InternalMatch, Long> {

    boolean existsByEmployee_IdAndDemandIdAndIsDeletedFalse(Long employeeId, Long demandId);

    long countByEmployee_IdAndIsDeletedFalse(Long employeeId);

    /**
     * Counts only ACCEPTED matches (actual allocations) for an engineer.
     * This enforces the "2-demand maximum" rule correctly by counting only
     * approved allocations, not PENDING_REVIEW or REJECTED nominations.
     *
     * @param employeeId the internal employee ID
     * @return count of accepted (allocated) matches
     */
    @Query("SELECT COUNT(m) FROM InternalMatch m WHERE m.employee.id = :employeeId " +
           "AND m.isDeleted = false AND m.matchStatus = :matchStatus")
    long countByEmployee_IdAndMatchStatusAndIsDeletedFalse(
            @Param("employeeId") Long employeeId,
            @Param("matchStatus") MatchStatus matchStatus
    );

    /**
     * Retrieves matches for a demand filtered by specific status.
     * Use this instead of findByDemandIdAndIsDeletedFalse() + Java filtering.
     *
     * @param demandId the demand ID
     * @param matchStatus the match status to filter by
     * @return list of matches with specified status
     */
    List<InternalMatch> findByDemandIdAndMatchStatusAndIsDeletedFalse(Long demandId, MatchStatus matchStatus);

    /**
     * Retrieves matches for an engineer filtered by specific status.
     * Use this to get only ACCEPTED or only PENDING_REVIEW matches.
     *
     * @param employeeId the internal employee ID
     * @param matchStatus the match status to filter by
     * @return list of matches with specified status
     */
    List<InternalMatch> findByEmployee_IdAndMatchStatusAndIsDeletedFalse(Long employeeId, MatchStatus matchStatus);

    /**
     * Retrieves matches for an engineer filtered by multiple statuses.
     * Use this to get active nominations (ACCEPTED + PENDING_REVIEW) excluding REJECTED.
     *
     * @param employeeId the internal employee ID
     * @param statuses list of match statuses to include
     * @return list of matches with any of the specified statuses
     */
    List<InternalMatch> findByEmployee_IdAndMatchStatusInAndIsDeletedFalse(Long employeeId, List<MatchStatus> statuses);

    /**
     * Legacy method - retrieves ALL matches regardless of status.
     * Consider using status-filtered methods above for better performance.
     */
    List<InternalMatch> findByDemandIdAndIsDeletedFalse(Long demandId);

    /**
     * Legacy method - retrieves ALL matches regardless of status.
     * Consider using status-filtered methods above for better performance.
     */
    List<InternalMatch> findByEmployee_IdAndIsDeletedFalse(Long employeeId);
}