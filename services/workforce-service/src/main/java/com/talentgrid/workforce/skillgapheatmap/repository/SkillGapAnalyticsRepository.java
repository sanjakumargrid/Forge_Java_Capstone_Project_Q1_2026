package com.talentgrid.workforce.skillgapheatmap.repository;

import com.talentgrid.workforce.skillgapheatmap.entity.SkillGapAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SkillGapAnalyticsRepository extends JpaRepository<SkillGapAnalytics, Long> {

    List<SkillGapAnalytics> findByCalculatedAt(LocalDateTime calculatedAt);

    @Query("""
           SELECT MAX(s.calculatedAt)
           FROM SkillGapAnalytics s
           """)
    Optional<LocalDateTime> findLatestSnapshotTime();

    List<SkillGapAnalytics> findBySkillNameOrderByCalculatedAtDesc(String skillName);

    /**
     * Deletes all snapshots older than the given cutoff — used for data retention.
     */
    @Modifying
    @Transactional
    @Query("""
           DELETE FROM SkillGapAnalytics s
           WHERE s.calculatedAt < :cutoff
           """)
    int deleteByCalculatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
