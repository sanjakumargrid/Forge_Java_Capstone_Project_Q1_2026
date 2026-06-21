package com.talentgrid.demand.repository;

import com.talentgrid.demand.domain.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findAllByOrderBySkillNameAsc();

    @Query(value = "SELECT * FROM skills ORDER BY embedding <=> CAST(:vector AS vector) LIMIT :limit", nativeQuery = true)
    List<Skill> findNearestSkills(@Param("vector") String vector, @Param("limit") int limit);

    List<Skill> findAllByEmbeddingIsNotNull();
}
