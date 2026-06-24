package com.talentgrid.interview.analytics.repository;

import com.talentgrid.interview.analytics.entity.RecruitmentAnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public interface AnalyticsSnapshotRepository extends JpaRepository<RecruitmentAnalyticsSnapshot, Long> {

    Optional<RecruitmentAnalyticsSnapshot> findFirstByDemandIdOrderByCalculatedAtDesc(Long demandId);

    @Query("SELECT r FROM RecruitmentAnalyticsSnapshot r WHERE r.demandId IS NULL ORDER BY r.calculatedAt DESC LIMIT 1")
    Optional<RecruitmentAnalyticsSnapshot> findLatestGlobalSnapshot();

    /**
     * Aggregates live transaction telemetry metrics straight from application and offer workflows.
     */
    @Query(value = """
        SELECT 
            COUNT(application_id) as total_apps,
            COUNT(CASE WHEN applied_at IS NOT NULL THEN 1 END) as count_applied,
            COUNT(CASE WHEN screening_at IS NOT NULL THEN 1 END) as count_screening,
            COUNT(CASE WHEN technical_at IS NOT NULL THEN 1 END) as count_technical,
            COUNT(CASE WHEN interview_at IS NOT NULL THEN 1 END) as count_interview,
            COUNT(CASE WHEN final_round_at IS NOT NULL THEN 1 END) as count_final,
            COUNT(CASE WHEN offer_at IS NOT NULL THEN 1 END) as count_offer,
            COUNT(CASE WHEN hired_at IS NOT NULL THEN 1 END) as count_hired,
            
            -- Phase Gap Interval Averages (Converted from seconds to fractional days)
            AVG(EXTRACT(EPOCH FROM (screening_at - applied_at))) / 86400 as avg_applied_to_screening,
            AVG(EXTRACT(EPOCH FROM (technical_at - screening_at))) / 86400 as avg_screening_to_technical,
            AVG(EXTRACT(EPOCH FROM (interview_at - technical_at))) / 86400 as avg_technical_to_interview,
            AVG(EXTRACT(EPOCH FROM (final_round_at - interview_at))) / 86400 as avg_interview_to_final,
            AVG(EXTRACT(EPOCH FROM (offer_at - final_round_at))) / 86400 as avg_final_to_offer,
            AVG(EXTRACT(EPOCH FROM (hired_at - offer_at))) / 86400 as avg_offer_to_hired
        FROM application
        WHERE (:demandId IS NULL OR demand_id = :demandId)
    """, nativeQuery = true)
    Map<String, Object> getRawLiveApplicationMetrics(@Param("demandId") Long demandId);

    /**
     * Calculates current offer sign-off completion metrics
     */
    @Query(value = """
        SELECT 
            COUNT(offer_id) as total_offers,
            COUNT(CASE WHEN offer_status = 'SIGNED' THEN 1 END) as signed_offers
        FROM offer o
        INNER JOIN application a ON o.application_id = a.application_id
        WHERE (:demandId IS NULL OR a.demand_id = :demandId)
    """, nativeQuery = true)
    Map<String, Object> getRawLiveOfferMetrics(@Param("demandId") Long demandId);
}
