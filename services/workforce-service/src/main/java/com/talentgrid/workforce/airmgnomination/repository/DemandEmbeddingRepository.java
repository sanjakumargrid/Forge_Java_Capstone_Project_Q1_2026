package com.talentgrid.workforce.airmgnomination.repository;

import com.talentgrid.workforce.airmgnomination.entity.DemandEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link DemandEmbedding}.
 *
 * <p>Uses {@link #findByDemandId(Long)} to support upsert logic — if a row already exists
 * for a demand, its embedding and summary are refreshed; otherwise a new row is inserted.
 */
@Repository
public interface DemandEmbeddingRepository extends JpaRepository<DemandEmbedding, Long> {

    /**
     * Lookup by the business-level demandId (not the DB primary key).
     */
    Optional<DemandEmbedding> findByDemandId(Long demandId);
}
