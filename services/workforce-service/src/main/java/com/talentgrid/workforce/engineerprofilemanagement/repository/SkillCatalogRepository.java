package com.talentgrid.workforce.engineerprofilemanagement.repository;

import com.talentgrid.workforce.engineerprofilemanagement.entity.SkillCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillCatalogRepository extends JpaRepository<SkillCatalogEntry, Long> {

    Optional<SkillCatalogEntry> findBySkillNameIgnoreCase(String skillName);

    List<SkillCatalogEntry> findAllByOrderBySkillNameAsc();
}
