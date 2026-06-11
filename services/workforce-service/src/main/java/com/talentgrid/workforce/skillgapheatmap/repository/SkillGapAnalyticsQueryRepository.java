package com.talentgrid.workforce.skillgapheatmap.repository;

import com.talentgrid.workforce.skillgapheatmap.entity.SkillGapAnalytics;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Custom JPQL repository for queries that Spring Data cannot derive
 * automatically — specifically, fetching all rows for the latest snapshot time.
 */
@Repository
public class SkillGapAnalyticsQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Returns all SkillGapAnalytics rows for the most recent snapshot time.
     * Two JPQL queries: one to find the max calculatedAt, one to fetch rows.
     */
    public List<SkillGapAnalytics> getLatestSnapshot() {
        LocalDateTime latest = getLatestSnapshotTime();
        if (latest == null) {
            return List.of();
        }
        return em.createQuery(
                        "SELECT s FROM SkillGapAnalytics s WHERE s.calculatedAt = :ts ORDER BY s.skillName ASC",
                        SkillGapAnalytics.class)
                .setParameter("ts", latest)
                .getResultList();
    }

    /**
     * Returns the timestamp of the most recent snapshot, or null if no data exists.
     */
    public LocalDateTime getLatestSnapshotTime() {
        return em.createQuery(
                        "SELECT MAX(s.calculatedAt) FROM SkillGapAnalytics s",
                        LocalDateTime.class)
                .getSingleResult();
    }

    /**
     * Returns all rows for the two most recent snapshot times.
     * Uses two portable JPQL queries instead of LIMIT inside a subquery.
     */
    public List<SkillGapAnalytics> findLatestTwoSnapshotsForAllSkills() {
        List<LocalDateTime> snapshotTimes = em.createQuery(
                        "SELECT DISTINCT s.calculatedAt FROM SkillGapAnalytics s ORDER BY s.calculatedAt DESC",
                        LocalDateTime.class)
                .setMaxResults(2)
                .getResultList();

        if (snapshotTimes.isEmpty()) {
            return List.of();
        }

        return em.createQuery(
                        "SELECT s FROM SkillGapAnalytics s WHERE s.calculatedAt IN :times "
                                + "ORDER BY s.skillName ASC, s.calculatedAt DESC",
                        SkillGapAnalytics.class)
                .setParameter("times", snapshotTimes)
                .getResultList();
    }
}
